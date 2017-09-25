package sentinel.camera.camera.module

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.google.inject.AbstractModule
import org.bytedeco.javacv.Frame
import sentinel.camera.camera.actor.CameraSourceActor

class CameraModule extends AbstractModule {
  override def configure(): Unit = {

    bind(classOf[Source[Int, Cancellable]]).toProvider(classOf[TickSourceProvider])
    bind(classOf[Source[Frame, NotUsed]]).toProvider(classOf[CameraSourceProvider])
//    bind(classOf[CameraReaderGraphFactory]).toInstance(classOf[CameraSourceActor])
  }
}
