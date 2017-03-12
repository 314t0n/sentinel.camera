package sentinel.camera.webcam

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.contrib.throttle.Throttler.RateInt
import akka.contrib.throttle.TimerBasedThrottler
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import org.bytedeco.javacpp.opencv_core.CV_8U
import org.bytedeco.javacv.FrameGrabber.ImageMode
import org.bytedeco.javacv.{Frame, FrameGrabber}
import sentinel.camera.utils.Dimensions
import sentinel.camera.webcam.actor.WebcamFramePublisher

import scala.concurrent.duration._

object WebCamera {

  private val OneSecond = 1.0
  private val MillisecondsPerSecond = 1000
  val DefaultFrameRate = 30

  /**
    * Builds a Frame [[Source]]
    *
    * @param deviceId device ID for the webcam
    * @param dimensions
    * @param bitsPerPixel
    * @param imageMode
    * @param system   ActorSystem
    * @return a Source of [[Frame]]s
    */
  def source(
              deviceId: Int,
              dimensions: Dimensions,
              bitsPerPixel: Int = CV_8U,
              imageMode: ImageMode = ImageMode.COLOR,
              framePerSec: Int = DefaultFrameRate
            )(implicit system: ActorSystem): Source[Frame, NotUsed] = {

    lazy val grabber = buildGrabber(
      deviceId = deviceId,
      imageWidth = dimensions.width,
      imageHeight = dimensions.height,
      bitsPerPixel = bitsPerPixel,
      imageMode = imageMode
    )

    val frameRate = (OneSecond / framePerSec) * MillisecondsPerSecond

    val throttler = system.actorOf(Props(
      classOf[TimerBasedThrottler],
      1 msgsPer frameRate.millisecond))

    val props = Props(new WebcamFramePublisher(grabber, throttler))
    val webcamActorRef = system.actorOf(props)
    val webcamActorPublisher = ActorPublisher[Frame](webcamActorRef)

    Source.fromPublisher(webcamActorPublisher)
  }

  //TODO make it testable
  private def buildGrabber(deviceId: Int,
                           imageWidth: Int,
                           imageHeight: Int,
                           bitsPerPixel: Int,
                           imageMode: ImageMode): FrameGrabber = synchronized {
    val grabber = FrameGrabber.createDefault(deviceId)
    grabber.setImageWidth(imageWidth)
    grabber.setImageHeight(imageHeight)
    grabber.setBitsPerPixel(bitsPerPixel)
    grabber.setImageMode(imageMode)
    grabber.start
    grabber
  }
}
