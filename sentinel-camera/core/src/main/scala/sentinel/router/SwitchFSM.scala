package sentinel.router

import akka.actor.ActorRef
import akka.actor.FSM
import akka.actor.PoisonPill
import akka.actor.Props
import akka.util.Timeout
import sentinel.camera.camera.actor.CameraSourceActor
import sentinel.camera.camera.actor.CameraSourceActorFactory
import sentinel.camera.utils.settings.Settings
import sentinel.router.messages.Messages._
import sentinel.router.messages._
import akka.pattern.ask
import sentinel.camera.camera.reader.CameraReaderFactory
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object SwitchFSM {

  val Name = classOf[SwitchFSM].getName

  def props(cameraSourceFactory: CameraReaderFactory, settings: Settings)(implicit ec: ExecutionContext) =
    Props(new SwitchFSM(cameraSourceFactory, settings))

}

/**
  * Handling Active/Idle stages
  * shutdown killSwitch on Stop
  * delegate state changes to a router
  */
class SwitchFSM(cameraReaderFactory: CameraReaderFactory, settings: Settings)(implicit val ec: ExecutionContext)
    extends FSM[State, Request] {

  private val duration         = settings.getDuration("system.options.startUpTimeout")
  private implicit val timeout = Timeout(duration)

  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      context.child(CameraSourceActor.Name).map(context.stop(_))
      goto(Active)
    case Event(Error, _) =>
      goto(Idle)
    case Event(GoToIdle, _) =>
      goto(Idle)
  }

  when(Idle) {
    case Event(Start(ks), _) =>
      log.debug("Start request")
      cameraReaderFactory.create(ks)

      Try(cameraReaderFactory.create(ks).future.onComplete {
        case Success(_) => self ! GoToActive
        case Failure(e)  => self ! Error(e.getMessage)
      }) recover {
        case e: Exception => self ! Error(e.getMessage)
      }
      goto(Waiting) using (Start(ks))
  }

  when(Active) {
    case Event(Stop, _) =>
      goto(Idle)
  }

  private def askCameraToStart(actorRef: ActorRef, request: Request, requestor: ActorRef) = {
    log.debug("{} request sent to camera, timeout: {}", request, timeout)
    ask(actorRef, request)
      .mapTo[Response]
      .onComplete({
        case Success(Ready(msg)) =>
          log.debug("Camera responded successfully with: {}", msg)
          actorRef ! PoisonPill
          self ! GoToActive
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
      })
  }

  onTransition {
    case Waiting -> Idle =>
      stateData match {
        case Start(ks) =>
          ks.shutdown()
        case _ =>
          log.warning("received unhandled request {} in state Active", stateName)
      }
    case Active -> Idle =>
      stateData match {
        case Start(ks) =>
          ks.shutdown()
          sender() ! Ready(Finished)
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
