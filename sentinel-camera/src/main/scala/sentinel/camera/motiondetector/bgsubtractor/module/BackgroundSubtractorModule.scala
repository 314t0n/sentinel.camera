package sentinel.camera.motiondetector.bgsubtractor.module

import com.google.inject.AbstractModule
import org.bytedeco.javacpp.opencv_video.BackgroundSubtractorMOG2
import sentinel.camera.motiondetector.bgsubtractor.GaussianMixtureBasedBackgroundSubstractor

class BackgroundSubtractorModule extends AbstractModule {
  override def configure(): Unit = {

    bind(classOf[BackgroundSubtractorMOG2])
      .toProvider(classOf[BackgroundSubstractorMOG2Provider])
    bind(classOf[GaussianMixtureBasedBackgroundSubstractor])
      .toProvider(classOf[GaussianMixtureBasedBackgroundSubstractorProvider])

  }
}
