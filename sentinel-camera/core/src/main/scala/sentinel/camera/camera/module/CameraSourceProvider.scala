package sentinel.camera.camera.module

import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.name.Named
import sentinel.camera.camera.actor.CameraSourceActor
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory

class CameraSourceProvider @Inject()(
    system: ActorSystem,
    @Named("CameraReaderFactory") cameraReaderFactory: CameraReaderGraphFactory,
    broadcastFactory: SourceBroadCastFactory
) extends Provider[ActorRef] {

  override def get(): ActorRef =
    system.actorOf(
      CameraSourceActor.props(cameraReaderFactory, broadcastFactory))
}
