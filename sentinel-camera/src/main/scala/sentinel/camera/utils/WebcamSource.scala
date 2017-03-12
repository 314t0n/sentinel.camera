package sentinel.camera.utils

import akka.NotUsed
import akka.actor.{ActorLogging, ActorSystem, Cancellable, DeadLetterSuppression, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.Source
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacv.{Frame, FrameGrabber}
import org.bytedeco.javacv.FrameGrabber.ImageMode
import org.reactivestreams.Publisher
import akka.contrib.throttle.TimerBasedThrottler
import akka.contrib.throttle.Throttler.{RateInt, SetTarget}

import scala.concurrent.duration._

/**
  * Created by Lloyd on 2/13/16.
  */

object Webcam {

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
              imageMode: ImageMode = ImageMode.COLOR
            )(implicit system: ActorSystem): Source[Frame, NotUsed] = {
    //            )(implicit system: ActorSystem): Publisher[Frame] = {
    val props = Props(
      new WebcamFramePublisher(
        deviceId = deviceId,
        imageWidth = dimensions.width,
        imageHeight = dimensions.height,
        bitsPerPixel = bitsPerPixel,
        imageMode = imageMode
      )
    )

    val webcamActorRef = system.actorOf(props)
    val webcamActorPublisher = ActorPublisher[Frame](webcamActorRef)

    //    webcamActorPublisher
    Source.fromPublisher(webcamActorPublisher)
  }

  // Building a started grabber seems finicky if not synchronised; there may be some freaky stuff happening somewhere.
  private def buildGrabber(
                            deviceId: Int,
                            imageWidth: Int,
                            imageHeight: Int,
                            bitsPerPixel: Int,
                            imageMode: ImageMode
                          ): FrameGrabber = synchronized {
    val g = FrameGrabber.createDefault(deviceId)
    g.setImageWidth(imageWidth)
    g.setImageHeight(imageHeight)
    g.setBitsPerPixel(bitsPerPixel)
    g.setImageMode(imageMode)
    g.start()
    g
  }

  /**
    * Actor that backs the Akka Stream source
    */
  private class WebcamFramePublisher(
                                      deviceId: Int,
                                      imageWidth: Int,
                                      imageHeight: Int,
                                      bitsPerPixel: Int,
                                      imageMode: ImageMode
                                    )(implicit system: ActorSystem) extends ActorPublisher[Frame] with ActorLogging {

    private implicit val ec = context.dispatcher

    //     Lazy so that nothing happens until the flow begins
    private lazy val grabber = buildGrabber(
      deviceId = deviceId,
      imageWidth = imageWidth,
      imageHeight = imageHeight,
      bitsPerPixel = bitsPerPixel,
      imageMode = imageMode
    )

    private val throttler = system.actorOf(Props(
      classOf[TimerBasedThrottler],
      30 msgsPer 1.second))

    throttler ! SetTarget(Some(self))

    override def receive: Receive = {
      case _: Request => emitFrames()
      case Continue => emitFrames()
      case Cancel => onCompleteThenStop()
      case unexpectedMsg => log.warning(s"Unexpected message: $unexpectedMsg")
    }

    private def emitFrames(): Unit = {
      //
      if (isActive && totalDemand > 0) {
        /*
          Grabbing a frame is a blocking I/O operation, so we don't send too many at once.
         */
        grabFrame().foreach(onNext)
        if (totalDemand > 0) {
          throttler ! Continue
          //          self ! Continue
        }
      }
    }

    def grabFrame(): Option[Frame] = {
      Option(grabber.grab())
    }

    def close() = {
      throttler ! Cancel
    }
  }

  private case object Continue extends DeadLetterSuppression

}
