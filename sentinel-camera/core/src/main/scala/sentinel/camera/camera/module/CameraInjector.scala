package sentinel.camera.camera.module

import akka.actor.ActorRef
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.framegrabber.{FFmpegFrameGrabberBuilder, FrameGrabberBuilder}

class CameraInjector extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[CameraReaderGraphFactory])
      .annotatedWith(Names.named("CameraReaderFactory"))
      .toProvider(classOf[CameraReaderGraphFactoryProvider])

    bind(classOf[FrameGrabberBuilder]).to(classOf[FFmpegFrameGrabberBuilder])

    bind(classOf[ActorRef])
      .annotatedWith(Names.named("CameraSource"))
      .toProvider(classOf[CameraSourceProvider])
  }
}
