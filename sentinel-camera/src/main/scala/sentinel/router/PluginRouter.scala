package sentinel.router

import akka.actor.ActorRef
import akka.actor.FSM
import akka.routing.Router
import akka.routing.RoutingLogic
import akka.routing.SeveralRoutees
import sentinel.router.Messages._
import akka.pattern.ask
import akka.stream.KillSwitch
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.concurrent.duration._

class PluginRouter(cameraSource: ActorRef, routingLogic: RoutingLogic, routees: SeveralRoutees)(
  implicit val ec: ExecutionContext)
  extends FSM[State, Request] {

  private val duration = 2 seconds
  private implicit val timeout = Timeout(duration) // TODO config

  private val router = Router(routingLogic, routees.routees)

  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      goto(Active) using NoRequest

    case Event(GoToIdle, _) =>
      goto(Idle) using NoRequest

    case Event(Ready(Ok), WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToActive
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(Ready(Finished), WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToIdle
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(SourceInit(bs), WaitingForSource(sender, ks)) =>
      router.route(PluginStart(ks, bs), self)
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)
  }

  onTransition {
    case Waiting -> Active =>
      stateData match {
        case WaitingForRoutees(requestor, _) => requestor ! Ready(Ok)
      }

    case Waiting -> Idle =>
      stateData match {
        case WaitingForRoutees(requestor, _) => requestor ! Ready(Finished)
      }
  }

  when(Idle) {
    case Event(Start(ks), _) =>
      askSourceToStart(ks)
      goto(Waiting) using WaitingForSource(sender, ks)
  }

  private def askSourceToStart(ks: KillSwitch) = {
    ask(cameraSource, Start(ks))
      .mapTo[Response]
      .onComplete {
        case Success(SourceInit(bs)) =>
          self ! SourceInit(bs)
        case Failure(t) =>
          log.error("Error occurred while waiting for response: {}", t)
          self ! GoToIdle
      }
  }

  when(Active) {
    case Event(Stop, _) =>
      router.route(Stop, self)
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)
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
      log.warning("received unhandled request {} in state {}/{}, sender: {}", e, stateName, s, sender)
      stay
  }

  initialize()
}
