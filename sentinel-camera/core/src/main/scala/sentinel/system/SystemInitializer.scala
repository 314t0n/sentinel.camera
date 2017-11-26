package sentinel.system

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import sentinel.camera.camera.reader.BroadCastRunnableGraph
import sentinel.camera.camera.reader.BroadcastMaterializer
import sentinel.camera.utils.settings.Settings
import sentinel.router.messages.Messages.Ok
import sentinel.router.messages._

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

object SystemInitializer {

  val Name = "SystemInitializer"

  def props(broadCastMaterializer: BroadcastMaterializer, pluginRegistry: ActorRef, settings: Settings)(
      implicit ec: ExecutionContext): Props =
    Props(new SystemInitializer(broadCastMaterializer, pluginRegistry, settings))
}

class SystemInitializer @Inject()(broadCastMaterializer: BroadcastMaterializer,
                                  pluginRegistry: ActorRef,
                                  settings: Settings)(implicit val ec: ExecutionContext)
    extends Actor {

  private implicit val timeout = Timeout(settings.getDuration("system.options.startUpTimeout"))

  override def receive: Receive = {
    case Start(gks) =>
      broadCastMaterializer.create(gks).future.onComplete {
        case Success(bs: BroadCastRunnableGraph) =>
          pluginRegistry ! AdvancedPluginStart(gks, bs)
          sender ! Status(Right(Ok))
        case Failure(t) =>
          sender ! Status(Left(t))
      }
  }
}
