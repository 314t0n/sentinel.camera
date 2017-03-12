package sentinel.camera.webcam.actor

import java.time.LocalDateTime

import akka.actor.{ActorLogging, ActorRef, ActorSystem, DeadLetterSuppression}
import akka.contrib.throttle.Throttler.SetTarget
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import org.bytedeco.javacv.{Frame, FrameGrabber}

import scala.util.Try

/**
  * Webcamera frame stream
  *
  * @param grabber   webcamera frame grabber
  * @param throttler TimerBasedThrottler for flow control
  */
class WebcamFramePublisher(grabber: FrameGrabber,
                           throttler: ActorRef)
                          (implicit system: ActorSystem)
  extends ActorPublisher[Frame]
    with ActorLogging
    with AutoCloseable {
  require(Option(grabber).isDefined)

  private implicit val ec = context.dispatcher

  throttler ! SetTarget(Some(self))

  override def receive: Receive = {
    case _: Request => {
      println(LocalDateTime.now().toString + "Request" + totalDemand + " " + isActive)
      throttler ! Continue
    }
    case Continue => {
      println(LocalDateTime.now().toString + "Continue")
      emitFrames()
    }
    case Cancel => {
      println(LocalDateTime.now().toString + "Cancel")
      onCompleteThenStop()
      Try(grabber.close)
    }
    case unexpectedMsg => log.warning(s"Unexpected message: $unexpectedMsg")
  }

  //TODO probably deprecated
  override def close(): Unit = throttler ! Cancel

  private def emitFrames(): Unit = synchronized {
    if (isActive && totalDemand > 0) {
      grabFrame().foreach(onNext)
//      if (totalDemand > 0) {
//        throttler ! Continue
//      }
    }
  }

  private def grabFrame(): Option[Frame] =
    Try(Option(grabber.grab())) recover { case e: Exception => None } get

  private case object Continue extends DeadLetterSuppression

}
