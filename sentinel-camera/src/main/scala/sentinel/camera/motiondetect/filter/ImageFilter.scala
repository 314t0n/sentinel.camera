package sentinel.camera.motiondetect.filter

import org.bytedeco.javacpp.opencv_core.Mat

trait ImageFilter {
  def filter(frame: Mat): Mat
}
