package sentinel.router.module

import akka.actor.ActorRef
import akka.actor.ActorSystem
import com.google.inject.Inject
import com.google.inject.Provider
import sentinel.camera.utils.settings.Settings
import sentinel.router.RouterFSM

class RouterFSMProvider @Inject()(system: ActorSystem, settings: Settings) extends Provider[ActorRef] {

  override def get(): ActorRef = system.actorOf(RouterFSM.props(settings)(system), RouterFSM.Name)

}
