package sentinel.camera.camera

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.Frame
import sentinel.camera.camera.stage.CameraReaderStage
import sentinel.camera.framegrabber.FrameGrabberBuilder
@deprecated
object Camera extends LazyLogging{
  /**
    * Builds a Frame [[Source]]
    *
    * @param frameGrabberBuilder Builds a FrameGrabber
    * @param system   ActorSystem
    * @return a Source of [[Frame]]s
    */
  def source(frameGrabberBuilder: FrameGrabberBuilder)(implicit system: ActorSystem): Source[Frame, NotUsed] = {
    logger.info("creating grabber")
    lazy val grabber = frameGrabberBuilder.create()
    Source.fromGraph(new CameraReaderStage(grabber))
  }
}