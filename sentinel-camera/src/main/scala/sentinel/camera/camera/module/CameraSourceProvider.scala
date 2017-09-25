package sentinel.camera.camera.module

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.Inject
import com.google.inject.Provider
import org.bytedeco.javacv.Frame
import sentinel.camera.camera.stage.CameraReaderStage
import sentinel.camera.framegrabber.FrameGrabberBuilder

class CameraSourceProvider @Inject()(frameGrabberBuilder: FrameGrabberBuilder)
    extends Provider[Source[Frame, NotUsed]] {

  override def get(): Source[Frame, NotUsed] = {
    lazy val grabber = frameGrabberBuilder.create()
    Source.fromGraph(new CameraReaderStage(grabber))
  }
}
