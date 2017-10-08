package sentinel.camera.utils.webcam.module

import com.google.inject.AbstractModule
import sentinel.camera.utils.webcam.WebcamWindow

class WebcamModule extends AbstractModule {
  override def configure(): Unit = {

    bind(classOf[WebcamWindow])

  }
}
