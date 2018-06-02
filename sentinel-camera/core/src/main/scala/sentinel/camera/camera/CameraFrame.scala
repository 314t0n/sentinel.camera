package sentinel.camera.camera

import java.time.LocalDateTime
import org.bytedeco.javacpp.opencv_core.IplImage
// TODO change to MAT
case class CameraFrame(image: IplImage, date: LocalDateTime = LocalDateTime.now()) {
  def formattedDate(format: String): String = date.formatted(format)
}

case class MotionDetectFrame(masked: IplImage, originalFrame: CameraFrame)
