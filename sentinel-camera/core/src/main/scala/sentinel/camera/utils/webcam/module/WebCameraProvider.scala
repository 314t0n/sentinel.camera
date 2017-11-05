package sentinel.camera.utils.webcam.module

import javax.inject.Named

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Provider}
import org.bytedeco.javacv.Frame
import sentinel.camera.camera.stage.CameraReaderStage
import sentinel.camera.framegrabber.FrameGrabberBuilder
@deprecated
class WebCameraProvider @Inject()(frameGrabberBuilder: FrameGrabberBuilder)
                                 (implicit @Named("ActorSystem") system: ActorSystem)
  extends Provider[Source[Frame, NotUsed]] {

  override def get(): Source[Frame, NotUsed] = Source.fromGraph(new CameraReaderStage(frameGrabberBuilder.create))

}
