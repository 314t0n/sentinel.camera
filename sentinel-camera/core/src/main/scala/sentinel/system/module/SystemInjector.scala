package sentinel.system.module

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.AbstractModule

class SystemInjector(system: ActorSystem, materalizer: ActorMaterializer)
    extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ActorSystem]).toInstance(system)
    bind(classOf[ActorMaterializer]).toInstance(materalizer)
  }
}
