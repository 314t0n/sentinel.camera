package sentinel.camera.framegrabber

import javax.inject.Inject

import org.bytedeco.javacpp.avutil.AV_PIX_FMT_BGR24
import org.bytedeco.javacv.{FFmpegFrameGrabber, FrameGrabber}
import sentinel.camera.utils.settings.Settings

sealed trait FrameGrabberBuilder {
  def create(): FrameGrabber
}

class FFmpegFrameGrabberBuilder @Inject()(settings: Settings) extends FrameGrabberBuilder {

  override def create(): FrameGrabber = synchronized {
    val grabber = new FFmpegFrameGrabber(settings.cameraPath)
    grabber.setFormat(settings.cameraFormat)
    grabber.setFrameRate(settings.getInt("camera.fps"))
    grabber.setNumBuffers(30)
    settings.cameraOptions.foreach { case (k, v) => grabber.setOption(k, v.toString) }
    grabber.setPixelFormat(AV_PIX_FMT_BGR24)
    grabber
  }
}
