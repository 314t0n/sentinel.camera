package sentinel.router

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, FSM}
import akka.pattern.ask
import akka.routing.{Router, RoutingLogic, SeveralRoutees}
import akka.util.Timeout
import sentinel.camera.utils.settings.Settings
import sentinel.router.Messages._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class PluginRouter(cameraSource: ActorRef,
                   routingLogic: RoutingLogic,
                   routees: SeveralRoutees,
                   settings: Settings)(implicit val ec: ExecutionContext,
                                       val system: ActorSystem)
    extends FSM[State, Request] {

  private val routerTimeoutDuration = settings.getDuration("system.options.routerTimeout", TimeUnit.SECONDS)
  private val sourceTimeoutDuration = settings.getDuration("system.options.sourceTimeout", TimeUnit.SECONDS)
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

    case Event(SourceInit(bs), WaitingForSource(sender, Start(ks))) =>
      router.route(PluginStart(ks, bs), self)
      scheduleRouterTimeoutCheck
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)

    case Event(Ready(Finished), WaitingForSource(sender, Stop)) =>
      router.route(Stop, self)
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)

    case Event(RouterTimeouted, WaitingForRoutees(requestor, _)) =>
      router.route(Stop, self)
      log.error(s"Router timeouted after $routerTimeoutDuration")
      requestor ! Error(s"Router timeouted after $routerTimeoutDuration")
      goto(Idle) using Stop
  }

  private case object RouterTimeouted extends Request

  private def scheduleRouterTimeoutCheck = system.scheduler.scheduleOnce(routerTimeoutDuration) {
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
      askSource(Start(ks))
      goto(Waiting) using WaitingForSource(sender, Start(ks))
  }

  private def askSource(request: Request) =
    ask(cameraSource, request)(sourceTimeout)
      .mapTo[Response]
      .onComplete {
        case Success(request: Response) =>
          self ! request
        case Failure(t) =>
          log.error("Error occurred while waiting for response: {}", t)
          self ! Error(t.getMessage)
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
