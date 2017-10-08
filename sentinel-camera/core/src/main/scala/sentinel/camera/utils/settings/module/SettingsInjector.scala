package sentinel.camera.utils.settings.module

import com.google.inject.AbstractModule
import sentinel.camera.utils.settings.{PropertyBasedSettings, Settings}

/**
  * Settings DI.
  */
class SettingsInjector extends AbstractModule{
  override def configure(): Unit = {

    bind(classOf[Settings]).toProvider(classOf[SettingsProvider])

  }
}
