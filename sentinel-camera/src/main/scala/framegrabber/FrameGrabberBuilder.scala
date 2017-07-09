package framegrabber

import org.bytedeco.javacv.{FFmpegFrameGrabber, FrameGrabber}
import sentinel.camera.utils.settings.Settings

sealed trait FrameGrabberBuilder {
  def create(): FrameGrabber
}

class FFmpegFrameGrabberBuilder(settings: Settings) extends FrameGrabberBuilder {

  override def create(): FrameGrabber = synchronized {
    val grabber = new FFmpegFrameGrabber(settings.cameraPath)
    grabber.setFormat(settings.cameraFormat)
    settings.cameraOptions.foreach { case (k, v) => grabber.setOption(k, v) }
    grabber
  }
}
