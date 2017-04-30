package sentinel.camera.motiondetect.bgsubtractor

import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_video.createBackgroundSubtractorMOG2
import sentinel.camera.webcam.CameraFrame

import scala.util.Try

/**
  * Provides motion detector algorithm
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
class GaussianMixtureBasedBackgroundSubstractor(lengthOfHistory: Int = 200,
                                                threshold: Int = 20,
                                                shadowDetect: Boolean = false)
  extends BackgroundSubstractor {

  //  private val backgroundSubtractorMOG2: BackgroundSubtractorMOG2 =
  //    createBackgroundSubtractorMOG2(lengthOfHistory, threshold, shadowDetect)
  //  private val learningRate = 1.0 / lengthOfHistory
  //  private val mask: Mat = new Mat()
  //
  //  def grayFilter(frame: Mat): Mat = {
  //    val grayFrame = new Mat()
  //    extractChannel(frame, grayFrame, 0)
  //    grayFrame
  //  }

  def subtractBackground(source: IplImage): IplImage = {
    val mtx = new Mat(source)
    val sourceSingle = cvCreateImage(cvGetSize(source), IPL_DEPTH_8U, 1)
    val fgMask = new Mat(sourceSingle)
    val backgroundSubtractorMOG2 = createBackgroundSubtractorMOG2
    backgroundSubtractorMOG2.apply(mtx, fgMask)
    val img = new IplImage(fgMask)
    sourceSingle.release()
    mtx.release()
    fgMask.release()
    backgroundSubtractorMOG2.close()
    img
  }

  //  private val structuringElement: Mat = getStructuringElement(MORPH_RECT, new Size(5, 5)) // todo parameter
  //
  //  def edgeFilter(frame: Mat): Mat = {
  //    val destionationFrame = new Mat(frame.rows(), frame.cols(), frame.`type`())
  //    morphologyEx(frame, destionationFrame, MORPH_OPEN, structuringElement)
  //    destionationFrame
  //  }

  override def substractBackground(frame: CameraFrame): CameraFrame = {
    Try(CameraFrame(subtractBackground(frame.image))) recover {
      case e: Exception => {
        println("GMB " + e)
        new CameraFrame(new IplImage())
      }
    } get

    //    backgroundSubtractorMOG2.apply(toMat(frame.image), mask, learningRate)
    //    val mat = grayFilter(edgeFilter(mask))
    //    frame
  }
}
