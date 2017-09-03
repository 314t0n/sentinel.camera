package sentinel.camera.utils.settings

import com.typesafe.config.Config

import scala.collection.JavaConverters._
import scala.util.Try

sealed trait Settings {
  /**
    * Camera path on the OS.
    *
    * @return
    */
  def cameraPath(): String

  /**
    * Video format that is used by FrameGrabber.
    * Although it's not tied to ffmpeg, it is the default FrameGrabber.
    * Depending on OS (for ffmpeg it's dshow on Windows, video4linux on Linux).
    *
    * @see https://ffmpeg.org/ffmpeg-devices.html
    * @return
    */
  def cameraFormat(): String

  /**
    * Additional options for the FrameGrabber.
    *
    * @return key value pairs
    */
  def cameraOptions(): Map[String, AnyRef]

  /**
    * Options for BackgroundSubstractorMog2.
    *
    * @return key value pairs
    */
  def motionDetectOptions(): Map[String, AnyRef]
}

/**
  * Settings defined in a property file.
  *
  * @param config typesafehub's Config
  * @see https://github.com/typesafehub/config
  */
class PropertyBasedSettings(config: Config) extends Settings {
  override def cameraPath(): String = config.getString("camera.path")

  override def cameraFormat(): String = config.getString("camera.ffmpeg.format")

  private def options = Try(Some(config.getObject("camera.options"))).recover { case _ => None }.get

  override def cameraOptions(): Map[String, AnyRef] =
    options
      .map(f => f.unwrapped.asScala.toMap)
      .getOrElse(Map.empty)

  override def motionDetectOptions(): Map[String, String] = Map()
}