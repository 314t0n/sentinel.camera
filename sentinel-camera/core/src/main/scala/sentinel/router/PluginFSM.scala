package sentinel.router

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.FSM
import akka.actor.Props
import akka.routing.Router
import akka.routing.RoutingLogic
import akka.routing.SeveralRoutees
import akka.stream.KillSwitch
import akka.util.Timeout
import sentinel.camera.camera.reader.BroadCastRunnableGraph
import sentinel.camera.utils.settings.Settings
import sentinel.router.Messages._

import scala.concurrent.ExecutionContext

object PluginFSM {

  val Name = classOf[PluginFSM].getName

  def props(
      cameraSource: ActorRef,
      routingLogic: RoutingLogic,
      routees: SeveralRoutees,
      settings: Settings)(implicit ec: ExecutionContext, system: ActorSystem) =
    Props(new PluginFSM(routingLogic, routees, settings)(ec, system))

}

/**
  * Route Start/Stop messages to Plugins
  *
  * @param routingLogic
  * @param routees
  * @param settings
  * @param ec
  * @param system
  */
class PluginFSM(routingLogic: RoutingLogic,
                routees: SeveralRoutees,
                settings: Settings)(implicit val ec: ExecutionContext,
                                    val system: ActorSystem)
    extends FSM[State, Request] {

  private implicit val pluginTimeout = Timeout(
    settings.getDuration("system.options.pluginsTimeout", TimeUnit.SECONDS))
  private lazy val numberOfRoutees = routees.routees.size - 1

  private val router = Router(routingLogic, routees.routees)
  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      goto(Active) using NoRequest

    case Event(GoToIdle, _) =>
      goto(Idle) using Stop

    case Event(Ready(Ok), WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToActive
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(Ready(Finished),
               WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToIdle
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(RouterTimeouted, WaitingForRoutees(requestor, _)) =>
      router.route(Stop, self)
      log.error(s"Router timeouted after ${settings
        .getDuration("system.options.pluginTimeout", TimeUnit.SECONDS)}")
      requestor ! Error(s"Router timeouted after ${settings
        .getDuration("system.options.pluginTimeout", TimeUnit.SECONDS)}")
      goto(Idle) using Stop
  }

  private def stopRoutees() = {
    if (routees.routees.nonEmpty) {
      router.route(Stop, self)
      // TODO: check why routee timeout missing here
    } else self ! GoToIdle
  }

  private def startRoutees(broadcast: BroadCastRunnableGraph, ks: KillSwitch) = {
    if (routees.routees.nonEmpty) {
      router.route(PluginStart(ks, broadcast), self)
      scheduleRouterTimeoutCheck
    } else self ! GoToActive
  }

  private case object RouterTimeouted extends Request

  private def scheduleRouterTimeoutCheck =
    system.scheduler.scheduleOnce(
      settings.getDuration("system.options.pluginTimeout", TimeUnit.SECONDS)) {
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
      }
  }

  when(Idle) {
    case Event(PluginStart(killSwitch, broadcast), _) =>
      log.debug("Start request")
      startRoutees(broadcast, killSwitch)
      goto(Waiting) using WaitingForRoutees(sender, numberOfRoutees)
  }

  when(Active) {
    case Event(Stop, _) =>
      stopRoutees()
      goto(Waiting) using WaitingForRoutees(sender, numberOfRoutees)
  }

  whenUnhandled {
    case Event(PluginStart(_, _), NoRequest) =>
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
