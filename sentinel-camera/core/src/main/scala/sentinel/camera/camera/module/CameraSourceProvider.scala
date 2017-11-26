package sentinel.camera.camera.module

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.google.inject.{Inject, Provider}
import sentinel.camera.camera.actor.CameraSourceActor
import sentinel.camera.camera.reader.BroadcastMaterializer

class CameraSourceProvider @Inject()(
    system: ActorSystem,
    materalizer: ActorMaterializer,
    broadCastMateralizer: BroadcastMaterializer
) extends Provider[ActorRef] {

  override def get(): ActorRef =
    system.actorOf(
      CameraSourceActor
        .props(broadCastMateralizer, materalizer))
}
