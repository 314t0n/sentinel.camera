package sentinel.router.module

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Provider}
import com.google.inject.name.Named
import sentinel.camera.utils.settings.Settings
import sentinel.router.Switch

import scala.concurrent.ExecutionContext

class SwitchProvider @Inject()(
    @Named("PluginRouter") router: ActorRef,
    settings: Settings,
    system: ActorSystem,
    @Named("MessageExecutionContext") ec: ExecutionContext
) extends Provider[ActorRef] {

  override def get(): ActorRef =
    system.actorOf(Switch.props(router, settings)(ec), "Switch")

}
