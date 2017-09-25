package sentinel.camera.config

import com.google.inject.Guice
import sentinel.camera.camera.module.CameraModule
import sentinel.camera.motiondetector.bgsubtractor.module.BackgroundSubtractorModule
import sentinel.camera.utils.settings.module.SettingsModule

object ModuleInjector {
  def apply(): ModuleInjector = new ModuleInjector()
}

class ModuleInjector {
  val injector = Guice.createInjector(
    new SettingsModule(),
    new BackgroundSubtractorModule(),
    new CameraModule()
  )
}
