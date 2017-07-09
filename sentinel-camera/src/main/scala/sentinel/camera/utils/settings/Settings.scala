package sentinel.camera.utils.settings

import com.typesafe.config.Config

sealed trait Settings {
  def cameraPath(): String

  def cameraFormat(): String

  def cameraOptions(): Map[String, String]
}

class PropertyBasedSettings(config: Config) extends Settings {

  override def cameraPath(): String = config.getString("camera.path")

  override def cameraFormat(): String = config.getString("camera.ffmpeg.format")

  override def cameraOptions(): Map[String, String] = Map()
}