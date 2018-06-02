package sentinel.system.module

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import sentinel.communication.BasicCommunication
import sentinel.communication.Communication
import sentinel.communication.Notifier
import sentinel.communication.NotifierProvider
import sentinel.router.module.CameraFSMProvider
import sentinel.router.module.MessageExecutionContextProvider
import sentinel.router.module.RouterFSMProvider
import sentinel.system.SystemInitializer

import scala.concurrent.ExecutionContext

class SystemInjector(system: ActorSystem, materalizer: ActorMaterializer) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ActorSystem]).toInstance(system)
    bind(classOf[ActorMaterializer]).toInstance(materalizer)

    bind(classOf[ExecutionContext])
      .annotatedWith(Names.named("StartUpEC"))
      .toProvider(classOf[StartUpExecutionContextProvider])

    bind(classOf[Communication])
      .to(classOf[BasicCommunication])

    bind(classOf[ActorRef])
      .annotatedWith(Names.named("Notifier"))
      .toProvider(classOf[NotifierProvider])

    bind(classOf[ActorRef])
      .annotatedWith(Names.named(SystemInitializer.Name))
      .toProvider(classOf[SystemInitilaizerProvider])
      .asEagerSingleton()
  }
}
