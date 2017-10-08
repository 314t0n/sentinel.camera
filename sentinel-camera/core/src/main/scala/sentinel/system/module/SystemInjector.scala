package sentinel.system.module

import akka.actor.ActorSystem
import com.google.inject.AbstractModule

class SystemInjector(system: ActorSystem) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ActorSystem]).toInstance(system)
  }
}
