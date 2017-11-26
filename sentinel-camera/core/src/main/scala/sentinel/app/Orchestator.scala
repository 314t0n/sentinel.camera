package sentinel.app

import akka.actor.ActorRef
import akka.stream.KillSwitches
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch
import sentinel.camera.utils.settings.Settings
import sentinel.plugin.Plugin
import sentinel.router.RouterFSM.{Add, Remove}
import sentinel.router.messages.{Start, Stop}

import scala.concurrent.ExecutionContext

class Orchestator @Inject()(@Named("SwitchFSM") switch: ActorRef,
                            @Named("RouterFSM") pluginRegistry: ActorRef,
                            settings: Settings)(@Named("MessageExecutionContext") implicit val ec: ExecutionContext)
    extends LazyLogging {

  private implicit val timeout = Timeout(settings.getDuration("system.options.startUpTimeout"))

  def addPlugin(plugin: Plugin): Unit = pluginRegistry ! Add(plugin)

  def removePlugin(plugin: Plugin): Unit = pluginRegistry ! Remove(plugin)

  def start(): Unit = switch ! Start(createKillswitch)

  def stop(): Unit = switch ! Stop

  private def createKillswitch() = GlobalKillSwitch(KillSwitches.shared("switch"))
}
