package sentinel.camera.framegrabber

import javax.inject.Inject

import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacpp.avutil.AV_PIX_FMT_BGR24
import org.bytedeco.javacv.{FFmpegFrameGrabber, FrameGrabber}
import sentinel.camera.utils.settings.Settings

sealed trait FrameGrabberBuilder {
  def create(): FrameGrabber
}

class FFmpegFrameGrabberBuilder @Inject()(settings: Settings) extends FrameGrabberBuilder with LazyLogging{

  override def create(): FrameGrabber = synchronized {
    val grabber = new FFmpegFrameGrabber(settings.cameraPath)
    grabber.setFormat(settings.cameraFormat)
//    grabber.setFrameRate(settings.getInt("camera.fps"))
    grabber.setImageWidth(settings.getInt("camera.width"))
    grabber.setImageHeight(settings.getInt("camera.height"))
    grabber.setOption("-r", settings.getInt("camera.fps").toString)
//    grabber.setNumBuffers(30)
//    grabber.setNumBuffers(settings.getInt("camera.fps"))
    logger.info(s"FPS set to ${settings.getInt("camera.fps")}")
    logger.info(s"FPS framerate ${grabber.getFrameRate}")
    settings.cameraOptions.foreach { case (k, v) => grabber.setOption(k, v.toString) }
    grabber.setPixelFormat(AV_PIX_FMT_BGR24)
    grabber
  }
}
