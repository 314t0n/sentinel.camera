package sentinel.camera.webcam

import java.time.LocalDateTime

import akka.stream._
import akka.stream.stage._
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacpp.opencv_core
import org.bytedeco.javacpp.opencv_core.CvScalar
import org.bytedeco.javacv.{Frame, FrameGrabber}

import scala.util.Try

class WebcamStage(grabber: FrameGrabber) extends GraphStage[SourceShape[Frame]] with LazyLogging {

  val out = Outlet[Frame]("Webcam.out")
  val webcamShape = SourceShape.of(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(out, new OutHandler {
        override def onPull(): Unit = grabFrame().get.foreach(frame => push(out, frame))
      })

      override def postStop(): Unit = {
        grabber.close()
        logger.debug("Webcam stopped")
      }

      override def preStart(): Unit = {
        grabber.start()
        logger.debug("Webcam started")
      }

      private def grabFrame(): Try[Option[Frame]] =
        Try(Option(grabber.grab())) recover {
          case e: Exception => {
            logger.error("Error grabbing the camera frame: ", e)
            None
          }
        }
    }

  override def shape: SourceShape[Frame] = webcamShape
}
