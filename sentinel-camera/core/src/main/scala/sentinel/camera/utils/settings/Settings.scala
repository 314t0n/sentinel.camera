package sentinel.camera.utils.settings

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.collection.JavaConverters._
import scala.concurrent.duration._
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

  def getDuration(path: String, unit: TimeUnit): FiniteDuration

  def getInt(path: String): Int
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

  override def cameraOptions(): Map[String, AnyRef] =
    getOptionsMap("camera.options")

  override def motionDetectOptions(): Map[String, String] = Map()

  override def getDuration(path: String, unit: TimeUnit): FiniteDuration =
    nonNull(FiniteDuration(config.getDuration(path, unit), unit), path)

  private def options(path: String) =
    Try(Some(config.getObject(path))).recover { case _ => None }.get

  private def getOptionsMap(path: String) =
    options(path)
      .map(f => f.unwrapped.asScala.toMap)
      .getOrElse(Map.empty)

  override def getInt(path: String): Int = config.getInt(path)

  private def nonNull[T](value: T, path: String) = Option(value) match {
    case Some(v) => v
    case None    => throw new RuntimeException(s"Configuration $path is missing!")
  }
}
