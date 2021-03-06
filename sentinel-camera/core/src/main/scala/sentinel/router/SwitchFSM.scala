package sentinel.router

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, FSM, Props}
import akka.pattern.ask
import akka.util.Timeout
import sentinel.camera.utils.settings.Settings
import sentinel.router.messages.Messages._
import sentinel.router.messages._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object SwitchFSM {

  val Name = classOf[SwitchFSM].getName

  def props(camera: ActorRef, settings: Settings)(implicit ec: ExecutionContext) =
    Props(new SwitchFSM(camera, settings))

}

/**
  * Handling Active/Idle stages
  * shutdown killSwitch on Stop
  * delegate state changes to a router
  *
  * @param camera
  */
class SwitchFSM(camera: ActorRef, settings: Settings)(implicit val ec: ExecutionContext) extends FSM[State, Request] {

  private val duration =
    settings.getDuration("system.options.startUpTimeout", TimeUnit.SECONDS)
  private implicit val timeout = Timeout(duration)

  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      log.debug("Active")
      goto(Active)
    case Event(Error, _) =>
      goto(Idle)
    case Event(GoToIdle, _) =>
      goto(Idle)
  }

  when(Idle) {
    case Event(Start(ks), _) =>
      log.debug("Start request")
      askRouter(Start(ks), GoToActive, sender)
      goto(Waiting) using (Start(ks))
  }

  when(Active) {
    case Event(Stop, _) =>
      askRouter(Stop, GoToIdle, sender)
      goto(Waiting)
  }

  private def askRouter(request: Request, nextState: Request, requestor: ActorRef) = {
    log.debug("{} request sent to camera, timeout: {}", request, timeout)
    ask(camera, request)
      .mapTo[Response]
      .onComplete {
        case Success(Ready(msg)) =>
          log.debug("Camera responded with Success")
          self ! nextState
          requestor ! Ready(msg)
        case Success(Error(reason)) =>
          log.error("Camera responded with error message {}", reason)
          requestor ! Error(reason)
          self ! Error(reason)
        case Success(unknowMessage) =>
          log.warning("Camera responded with unknown message {}", unknowMessage)
          self ! Error(unknowMessage.toString)
        case Failure(e) =>
          log.debug(s"Camera responded with error $e")
          log.error("Error occurred while waiting for response: {}", e)
          self ! GoToIdle
          requestor ! Error(e.getMessage)
      }
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
