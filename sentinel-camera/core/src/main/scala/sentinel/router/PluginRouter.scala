package sentinel.router

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.FSM
import akka.actor.Props
import akka.pattern.ask
import akka.routing.Router
import akka.routing.RoutingLogic
import akka.routing.SeveralRoutees
import akka.stream.KillSwitch
import akka.util.Timeout
import sentinel.camera.utils.settings.Settings
import sentinel.router.Messages._

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

object PluginRouter {

  def props(
      cameraSource: ActorRef,
      routingLogic: RoutingLogic,
      routees: SeveralRoutees,
      settings: Settings)(implicit ec: ExecutionContext, system: ActorSystem) =
    Props(
      new PluginRouter(cameraSource, routingLogic, routees, settings)(ec,
                                                                      system))

}

class PluginRouter(cameraSource: ActorRef,
                   routingLogic: RoutingLogic,
                   routees: SeveralRoutees,
                   settings: Settings)(implicit val ec: ExecutionContext,
                                       val system: ActorSystem)
    extends FSM[State, Request] {

  private val routerTimeoutDuration =
    settings.getDuration("system.options.routerTimeout", TimeUnit.SECONDS)
  private val sourceTimeoutDuration =
    settings.getDuration("system.options.sourceTimeout", TimeUnit.SECONDS)
  private implicit val sourceTimeout = Timeout(sourceTimeoutDuration)

  private val router = Router(routingLogic, routees.routees)

  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      goto(Active) using NoRequest

    case Event(GoToIdle, _) =>
      goto(Idle) using Stop

    case Event(Error(reason), WaitingForSource(requestor, _)) =>
      requestor ! Error(reason)
      goto(Idle) using Stop

    case Event(Ready(Ok), WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToActive
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(Ready(Finished),
               WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToIdle
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(SourceInit(broadcast), WaitingForSource(sender, Start(ks))) =>
      startRoutees(broadcast, ks)
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)

    case Event(Ready(Finished), WaitingForSource(sender, Stop)) =>
      stopRoutees()
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)

    case Event(RouterTimeouted, WaitingForRoutees(requestor, _)) =>
      router.route(Stop, self)
      log.error(s"Router timeouted after $routerTimeoutDuration")
      requestor ! Error(s"Router timeouted after $routerTimeoutDuration")
      goto(Idle) using Stop
  }

  private def stopRoutees() = {
    if (routees.routees.nonEmpty) {
      router.route(Stop, self)
      // TODO: check why routee timeout missing here
    } else self ! GoToIdle
  }

  private def startRoutees(broadcast: CameraSourcePublisher, ks: KillSwitch) = {
    if (routees.routees.nonEmpty) {
      router.route(PluginStart(ks, broadcast), self)
      scheduleRouterTimeoutCheck
    } else self ! GoToActive
  }

  private case object RouterTimeouted extends Request

  private def scheduleRouterTimeoutCheck =
    system.scheduler.scheduleOnce(routerTimeoutDuration) {
      self ! RouterTimeouted
    }

  onTransition {
    case Waiting -> Active =>
      stateData match {
        case WaitingForRoutees(requestor, _) => requestor ! Ready(Ok)
      }

    case Waiting -> Idle =>
      stateData match {
        case WaitingForRoutees(requestor, _) =>
          requestor ! Ready(Finished)
        case WaitingForSource(_, _) =>
          log.error("PluginRouter timeouted while waiting for source in state")
      }
  }

  when(Idle) {
    case Event(Start(ks), _) =>
      log.debug("Start request")
      askSource(Start(ks))
      goto(Waiting) using WaitingForSource(sender, Start(ks))
  }

  private def askSource(request: Request) = {
    log.debug(s"Ask source to $request")
    ask(cameraSource, request)(sourceTimeout)
      .mapTo[Response]
      .onComplete {
        case Success(request: Response) =>
          log.debug(s"Source responded with $request")
          self ! request
        case Failure(e) =>
          log.debug(s"Source responded with error $e")
          log.error("Error occurred while waiting for response: {}", e)
          self ! Error(e.getMessage)
        case _ => log.debug(s"Unknown error happened")
      }
  }

  when(Active) {
    case Event(Stop, _) =>
      askSource(Stop)
      goto(Waiting) using WaitingForSource(sender, Stop)
  }

  whenUnhandled {
    case Event(Start(_), NoRequest) =>
      log.error(AlreadyStarted)
      sender() ! Error(AlreadyStarted)
      stay
    case Event(Stop, Stop) =>
      log.error(Finished)
      sender() ! Error(Finished)
      stay
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}, sender: {}",
                  e,
                  stateName,
                  s,
                  sender)
      stay
  }

  initialize()
}
