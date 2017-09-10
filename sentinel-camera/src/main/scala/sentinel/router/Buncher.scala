package sentinel.router

import akka.actor.FSM
import sentinel.router.Messages._

class Buncher() extends FSM[State, Request] {

  startWith(Idle, Stop)

  when(Idle) {
    case Event(Start(ks), _) =>
      goto(Active) using Start(ks)
  }

  onTransition {
    case Active -> Idle =>
      stateData match {
        case Start(ks) =>
          ks.shutdown()
          sender() ! Ready(Finished)
        case _ =>
          log.warning("received unhandled request {} in state Active", stateName)
      }
    case Idle -> Active =>
      stateData match {
        case Stop =>
          sender() ! Ready(Ok)
        case _ =>
          log.warning("received unhandled request {} in state Idle", stateName)
      }
  }

  when(Active) {
    case Event(Stop, _) =>
      goto(Idle) using Stop
  }

  whenUnhandled {
    case Event(Start(_), Start(_)) =>
      sender() ! Error(AlreadyStarted)
      stay
    case Event(Stop, Stop) =>
      sender() ! Error(Finished)
      stay
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}
