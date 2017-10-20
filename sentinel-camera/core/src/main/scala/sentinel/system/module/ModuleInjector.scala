package sentinel.system.module

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject._
import sentinel.camera.camera.module.CameraInjector
import sentinel.camera.motiondetector.bgsubtractor.module.BackgroundSubstractorInjector
import sentinel.camera.utils.settings.module.SettingsInjector
import sentinel.router.module.RouterInjector

class ModuleInjector(system: ActorSystem, materializer: ActorMaterializer) {
  val injector = Guice.createInjector(
    new SystemInjector(system, materializer),
    new SettingsInjector(),
    new BackgroundSubstractorInjector(),
    new CameraInjector(),
    new RouterInjector()
  )
}
