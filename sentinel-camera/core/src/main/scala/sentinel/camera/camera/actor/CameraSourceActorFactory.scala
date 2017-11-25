package sentinel.camera.camera.actor

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import sentinel.camera.camera.reader.BroadcastMateralizer

@deprecated
class CameraSourceActorFactory @Inject()(broadCastMateralizer: BroadcastMateralizer, materalizer: ActorMaterializer) {

  def create()(implicit context: ActorContext): ActorRef =
    context.actorOf(CameraSourceActor.props(broadCastMateralizer, materalizer), CameraSourceActor.Name)

}
