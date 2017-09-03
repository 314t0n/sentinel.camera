package sentinel.camera.camera

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import org.bytedeco.javacv.Frame
import sentinel.camera.camera.stage.CameraReaderStage
import sentinel.camera.framegrabber.FrameGrabberBuilder

object Camera {
  /**
    * Builds a Frame [[Source]]
    *
    * @param frameGrabberBuilder Builds a FrameGrabber
    * @param system   ActorSystem
    * @return a Source of [[Frame]]s
    */
  def source(frameGrabberBuilder: FrameGrabberBuilder)(implicit system: ActorSystem): Source[Frame, NotUsed] = {
    lazy val grabber = frameGrabberBuilder.create()
    Source.fromGraph(new CameraReaderStage(grabber))
  }
}