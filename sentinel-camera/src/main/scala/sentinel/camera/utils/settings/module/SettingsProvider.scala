package sentinel.camera.utils.settings.module

import com.google.inject.Provider
import sentinel.camera.utils.settings.{PropertyBasedSettingsLoader, Settings}

class SettingsProvider extends Provider[Settings] {
  override def get(): Settings = new PropertyBasedSettingsLoader().load()
}
