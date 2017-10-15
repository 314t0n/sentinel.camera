package sentinel.router.module

import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.name.Named
import sentinel.camera.utils.settings.Settings
import sentinel.router.SwitchFSM

import scala.concurrent.ExecutionContext

class SwitchProvider @Inject()(
    @Named("CameraFSM") camera: ActorRef,
    settings: Settings,
    system: ActorSystem,
    @Named("MessageExecutionContext") ec: ExecutionContext
) extends Provider[ActorRef] {

  override def get(): ActorRef =
    system.actorOf(SwitchFSM.props(camera, settings)(ec), SwitchFSM.Name)

}
