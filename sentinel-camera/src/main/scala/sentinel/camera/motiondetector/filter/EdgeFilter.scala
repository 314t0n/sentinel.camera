package sentinel.camera.motiondetector.filter

import org.bytedeco.javacpp.opencv_core.{Mat, Size}
import org.bytedeco.javacpp.opencv_imgproc.{MORPH_OPEN, MORPH_RECT, getStructuringElement, morphologyEx}

class EdgeFilter extends ImageFilter {
  private val structuringElement: Mat = getStructuringElement(MORPH_RECT, new Size(5, 5)) // todo parameter

  override def filter(frame: Mat): Mat = {
    val destionationFrame = new Mat(frame.rows(), frame.cols(), frame.`type`())
    morphologyEx(frame, destionationFrame, MORPH_OPEN, structuringElement)
    destionationFrame
  }
}
