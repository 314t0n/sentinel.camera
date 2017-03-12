package sentinel.camera.webcam

import java.time.LocalDateTime
import org.bytedeco.javacpp.opencv_core.IplImage

case class CameraFrame(image: IplImage,
                       date: LocalDateTime = LocalDateTime.now()) {
  def formattedDate(format: String): String = date.formatted(format)
}