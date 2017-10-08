package sentinel.camera.motiondetector.bgsubtractor

import org.bytedeco.javacpp.opencv_core.{IplImage, Mat}
import sentinel.camera.camera.CameraFrame
// todo consider moving these to some kind of common package
object BackgroundSubstractor{
  def toMat(iplImage: IplImage) = new Mat(iplImage)
  def toIplImage(mat: Mat) = new IplImage(mat)
}

trait BackgroundSubstractor extends AutoCloseable {
  def substractBackground(frame: CameraFrame): CameraFrame
}
