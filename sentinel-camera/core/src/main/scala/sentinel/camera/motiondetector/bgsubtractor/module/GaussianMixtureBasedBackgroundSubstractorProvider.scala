package sentinel.camera.motiondetector.bgsubtractor.module

import com.google.inject.{Inject, Provider}
import org.bytedeco.javacpp.opencv_video.BackgroundSubtractorMOG2
import sentinel.camera.motiondetector.bgsubtractor.GaussianMixtureBasedBackgroundSubstractor
import sentinel.camera.utils.settings.Settings

class GaussianMixtureBasedBackgroundSubstractorProvider @Inject()(backgroundSubtractorMOG2: BackgroundSubtractorMOG2,
                                                                  settings: Settings)
  extends Provider[GaussianMixtureBasedBackgroundSubstractor] {

  private val learningRate = settings.motionDetectOptions().getOrElse("learningRate", 0.1).asInstanceOf[Double]

  override def get(): GaussianMixtureBasedBackgroundSubstractor =
    GaussianMixtureBasedBackgroundSubstractor(backgroundSubtractorMOG2, learningRate)
}
