package sentinel.camera.utils.settings

import com.typesafe.config.{Config, ConfigFactory}
import SettingsLoader._

object SettingsLoader {
  val WindowsConf = "windows"
  val LinuxARMConf = "linux-arm"
}

trait SettingsLoader {
  def load(): Settings
}

class PropertyBasedSettingsLoader extends SettingsLoader {

  override def load(): Settings = {
    val conf: Config = ConfigFactory.load(determineConfigFile)
    new PropertyBasedSettings(conf)
  }

  private def os = System.getProperty("os.name")

  private def determineConfigFile =
    if (os.toLowerCase.startsWith("windows")) WindowsConf
    else LinuxARMConf
}
