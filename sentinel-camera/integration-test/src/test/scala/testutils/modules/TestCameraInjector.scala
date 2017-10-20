package testutils.modules

import com.google.inject.AbstractModule
import sentinel.camera.framegrabber.FrameGrabberBuilder

class TestCameraInjector(fgBuilder: FrameGrabberBuilder)
    extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[FrameGrabberBuilder]).toInstance(fgBuilder)
  }
}
