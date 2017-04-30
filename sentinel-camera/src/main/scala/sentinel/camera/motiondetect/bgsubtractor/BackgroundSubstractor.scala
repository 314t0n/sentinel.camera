package sentinel.camera.motiondetect.bgsubtractor

import org.bytedeco.javacpp.opencv_core.{IplImage, Mat}
import sentinel.camera.webcam.CameraFrame
// todo consider moving these to some kind of common package
object BackgroundSubstractor{
  def toMat(iplImage: IplImage) = new Mat(iplImage)
  def toIplImage(mat: Mat) = new IplImage(mat)
}

trait BackgroundSubstractor {
  def substractBackground(frame: CameraFrame): CameraFrame
}
