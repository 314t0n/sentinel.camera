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

class PluginFSMProvider @Inject()(
    system: ActorSystem,
    settings: Settings,
    @Named("CameraReaderFactory") cameraReaderFactory: CameraReaderGraphFactory,
    @Named("RouterFSM") router: ActorRef,
    @Named("MessageExecutionContext") ec: ExecutionContext
) extends Provider[ActorRef] {
  override def get(): ActorRef = {
    val routees = SeveralRoutees(Vector.empty)
    system.actorOf(PluginFSM.props(router, settings)(ec, system),
                   PluginFSM.Name)
  }
}
