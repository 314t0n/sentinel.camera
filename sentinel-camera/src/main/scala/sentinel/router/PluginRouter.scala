package sentinel.router

import akka.actor.FSM
import akka.routing.{Router, RoutingLogic, SeveralRoutees}
import sentinel.router.Messages._

class PluginRouter(routingLogic: RoutingLogic, routees: SeveralRoutees) extends FSM[State, Request] {

  private val router = Router(routingLogic, routees.routees)

  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      goto(Active) using NoRequest

    case Event(GoToIdle, _) =>
      goto(Idle) using NoRequest

    case Event(Ready(Ok), WaitingForResponse(requestor, numberOfResponses)) =>
      if (numberOfResponses == 0) self ! GoToActive
      stay() using WaitingForResponse(requestor, numberOfResponses - 1)

    case Event(Ready(Finished), WaitingForResponse(requestor, numberOfResponses)) =>
      if (numberOfResponses == 0) self ! GoToIdle
      stay() using WaitingForResponse(requestor, numberOfResponses - 1)
  }

  onTransition {
    case Waiting -> Active =>
      stateData match {
        case WaitingForResponse(requestor, _) => requestor ! Ready(Ok)
      }

    case Waiting -> Idle =>
      stateData match {
        case WaitingForResponse(requestor, _) => requestor ! Ready(Finished)
      }
  }

  when(Idle) {
    case Event(Start(ks), _) =>
      router.route(Start(ks), self)
      goto(Waiting) using WaitingForResponse(sender, router.routees.size - 1)
  }

  when(Active) {
    case Event(Stop, _) =>
      router.route(Stop, self)
      goto(Waiting) using WaitingForResponse(sender, router.routees.size - 1)
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
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}
