package sentinel.system.module

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import sentinel.router.module.{MessageExecutionContextProvider, RouterFSMProvider}
import sentinel.system.SystemInitializer

import scala.concurrent.ExecutionContext

class SystemInjector(system: ActorSystem, materalizer: ActorMaterializer)
    extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ActorSystem]).toInstance(system)
    bind(classOf[ActorMaterializer]).toInstance(materalizer)

    bind(classOf[ExecutionContext])
      .annotatedWith(Names.named("StartUpEC"))
      .toProvider(classOf[StartUpExecutionContextProvider])

    bind(classOf[ActorRef])
      .annotatedWith(Names.named(SystemInitializer.Name))
      .toProvider(classOf[SystemInitilaizerProvider])
      .asEagerSingleton()
  }
}
