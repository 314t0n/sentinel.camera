package sentinel.router.module

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.routing.BroadcastRoutingLogic
import akka.routing.SeveralRoutees
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.name.Named
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory
import sentinel.camera.utils.settings.Settings
import sentinel.router.PluginFSM

import scala.concurrent.ExecutionContext

class PluginRouterProvider @Inject()(
    system: ActorSystem,
    settings: Settings,
    broadcastFactory: SourceBroadCastFactory,
    @Named("CameraReaderFactory") cameraReaderFactory: CameraReaderGraphFactory,
    @Named("CameraSource") cameraSource: ActorRef,
    @Named("MessageExecutionContext") ec: ExecutionContext
) extends Provider[ActorRef] {
  override def get(): ActorRef = {
    val routees = SeveralRoutees(Vector.empty)
    system.actorOf(PluginFSM.props(cameraSource,
                                   BroadcastRoutingLogic(),
                                   routees,
                                   settings)(ec, system),
                   PluginFSM.Name)
  }
}
