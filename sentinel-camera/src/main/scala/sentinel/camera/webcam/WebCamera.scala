package sentinel.camera.webcam

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import org.bytedeco.javacv.Frame
import sentinel.camera.framegrabber.FrameGrabberBuilder

object WebCamera {
  /**
    * Builds a Frame [[Source]]
    *
    * @param frameGrabberBuilder Builds a FrameGrabber
    * @param system   ActorSystem
    * @return a Source of [[Frame]]s
    */
  def source(frameGrabberBuilder: FrameGrabberBuilder)(implicit system: ActorSystem): Source[Frame, NotUsed] = {
    lazy val grabber = frameGrabberBuilder.create()
    Source.fromGraph(new WebcamStage(grabber))
  }
}