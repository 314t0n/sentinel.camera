package sentinel.router.module

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Provider}
import com.google.inject.name.Named
import sentinel.camera.camera.reader.CameraReaderFactory
import sentinel.camera.utils.settings.Settings
import sentinel.router.SwitchFSM

import scala.concurrent.ExecutionContext

class SwitchProvider @Inject()(
                                cameraReaderFactory: CameraReaderFactory,
                                settings: Settings,
                                system: ActorSystem,
                                @Named("MessageExecutionContext") ec: ExecutionContext
) extends Provider[ActorRef] {

  override def get(): ActorRef =
    system.actorOf(SwitchFSM.props(cameraReaderFactory, settings)(ec), SwitchFSM.Name)

}
