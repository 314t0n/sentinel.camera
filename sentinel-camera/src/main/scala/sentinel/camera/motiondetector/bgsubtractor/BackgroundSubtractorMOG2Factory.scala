package sentinel.camera.motiondetector.bgsubtractor

import org.bytedeco.javacpp.opencv_video.{BackgroundSubtractorMOG2, createBackgroundSubtractorMOG2}

object BackgroundSubtractorMOG2Factory {
  /**
    *
    * @param lengthOfHistory number of frames used for motion detect
    * @param threshold       Threshold on the squared Mahalanobis distance between
    *                        the pixel and the model to decide whether a pixel is well described
    *                        by the background model. This parameter does not affect the
    *                        background update.
    * @param shadowDetect    If true, the algorithm will detect shadows and mark
    *                        them. It decreases the speed a bit, so if you do not need this
    *                        feature, set the parameter to false
    */
  def apply(lengthOfHistory: Int = 200,
            threshold: Int = 20,
            shadowDetect: Boolean = false): BackgroundSubtractorMOG2 =
    createBackgroundSubtractorMOG2(lengthOfHistory, threshold, shadowDetect)
}
