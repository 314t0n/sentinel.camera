package sentinel.router.module

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Provider}
import com.google.inject.name.Named
import sentinel.camera.utils.settings.Settings
import sentinel.router.CameraFSM

import scala.concurrent.ExecutionContext

class CameraFSMProvider @Inject()(
    system: ActorSystem,
    settings: Settings,
    @Named("PluginRouterFSM") router: ActorRef,
    @Named("CameraSource") cameraSource: ActorRef,
    @Named("MessageExecutionContext") ec: ExecutionContext
) extends Provider[ActorRef] {
  override def get(): ActorRef = {
    system.actorOf(CameraFSM.props(cameraSource, router, settings)(ec, system),
                   CameraFSM.Name)
  }

}
