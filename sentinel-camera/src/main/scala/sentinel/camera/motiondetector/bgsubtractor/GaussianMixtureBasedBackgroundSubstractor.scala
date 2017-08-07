package sentinel.camera.motiondetector.bgsubtractor

import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_video.BackgroundSubtractorMOG2
import sentinel.camera.webcam.CameraFrame
@deprecated
object GaussianMixtureBasedBackgroundSubstractor {
  @deprecated
  def apply(mog: BackgroundSubtractorMOG2, learningRate: Double): GaussianMixtureBasedBackgroundSubstractor
  = new GaussianMixtureBasedBackgroundSubstractor(mog, learningRate)
}

/**
  * Substracts foreground from background
  *
  * @param backgroundSubtractorMOG2
  * @param learningRate
  */
class GaussianMixtureBasedBackgroundSubstractor(backgroundSubtractorMOG2: BackgroundSubtractorMOG2, learningRate: Double)
  extends BackgroundSubstractor
    with LazyLogging {

  //  private val backgroundSubtractorMOG2: BackgroundSubtractorMOG2 =
  //    createBackgroundSubtractorMOG2(lengthOfHistory, threshold, shadowDetect)
  //  private val learningRate = 1.0 / lengthOfHistory
  private val mask: Mat = new Mat()
  //
  //  def grayFilter(frame: Mat): Mat = {
  //    val grayFrame = new Mat()
  //    extractChannel(frame, grayFrame, 0)
  //    grayFrame
  //  }

  private def applyMask(source: IplImage): IplImage = {
    val currentFrame = new Mat(source)
    backgroundSubtractorMOG2.apply(currentFrame, mask, learningRate)
    val maskedImage = new IplImage(mask)
    currentFrame.release()
    maskedImage
  }

  //  private val structuringElement: Mat = getStructuringElement(MORPH_RECT, new Size(5, 5)) // todo parameter
  //
  //  def edgeFilter(frame: Mat): Mat = {
  //    val destionationFrame = new Mat(frame.rows(), frame.cols(), frame.`type`())
  //    morphologyEx(frame, destionationFrame, MORPH_OPEN, structuringElement)
  //    destionationFrame
  //  }

  override def substractBackground(frame: CameraFrame): CameraFrame = CameraFrame(applyMask(frame.image))


  override def close(): Unit = {
    backgroundSubtractorMOG2.close()
    mask.release()
  }
}
