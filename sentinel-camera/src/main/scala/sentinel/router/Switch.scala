package sentinel.router

import akka.actor.{ActorRef, FSM}
import akka.pattern.ask
import akka.util.Timeout
import sentinel.router.Messages.{Stop, _}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Handling Active/Idle stages
  * shutdown killSwitch on Stop
  * delegate state changes to a router
  *
  * @param router
  */
class Switch(router: ActorRef)(implicit val ec: ExecutionContext) extends FSM[State, Request] {

  private val duration = 2 seconds
  private implicit val timeout = Timeout(duration) // TODO config

  private case object GoToActive extends Request

  private case object GoToIdle extends Request

  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      goto(Active)
    case Event(GoToIdle, _) =>
      goto(Idle)
  }

  when(Idle) {
    case Event(Start(ks), _) =>
      askRouter(Start(ks), GoToActive, sender)
      goto(Waiting) using (Start(ks))
  }

  when(Active) {
    case Event(Stop, _) =>
      askRouter(Stop, GoToIdle, sender)
      goto(Waiting)
  }

  private def askRouter(request: Request, nextState: Request, requestor: ActorRef) =
    ask(router, request).mapTo[Response]
      .onComplete {
        case Success(Ready(msg)) =>
          self ! nextState
          requestor ! Ready(msg)
        case Failure(t) =>
          log.error("Error occurred while waiting for response: {}", t)
          self ! GoToIdle
          requestor ! Error(t.getMessage)
      }

  onTransition {
    case Waiting -> Idle =>
      stateData match {
        case Start(ks) =>
          ks.shutdown()
        case _ =>
          log.warning("received unhandled request {} in state Active", stateName)
      }
  }

  whenUnhandled {
    case Event(Start(_), Start(_)) =>
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
