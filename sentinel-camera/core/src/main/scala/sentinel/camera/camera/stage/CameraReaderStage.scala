package sentinel.camera.camera.stage

import akka.stream._
import akka.stream.stage._
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import sentinel.router.messages.Messages.Error

import scala.util.Try

class CameraReaderStage(grabber: FrameGrabber)
    extends GraphStage[SourceShape[Frame]]
    with LazyLogging {

  val out = Outlet[Frame]("Camera.out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(out, new OutHandler {
        override def onPull(): Unit =
          grabFrame().get.foreach(frame => push(out, frame))
      })

      override def postStop(): Unit = {
        grabber.close()
        logger.debug("Camera stopped")
      }

      override def preStart(): Unit = {
        grabber.start()
        logger.debug("Camera started")
      }

      private def grabFrame(): Try[Option[Frame]] = {
        Try(Option(grabber.grab())) recover {
          case e: Exception => {
            logger.error("Error grabbing the camera frame: ", e)
            None
          }
        }
      }
    }

  override def shape: SourceShape[Frame] = SourceShape.of(out)
}
