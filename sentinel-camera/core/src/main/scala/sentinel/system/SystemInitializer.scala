import akka.actor.ActorRef
import sentinel.camera.camera.reader.{BroadcastMateralizer, CameraBroadcastInitializer}
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch
import sentinel.router.messages.{AdvancedPluginStart, Error, PluginStart}

import scala.util.{Failure, Success, Try}

class SystemInitializer(broadCastMateralizer: BroadcastMateralizer,
                        pluginRegistry: ActorRef) {

  def init(sender: ActorRef, gks: GlobalKillSwitch) = {
    initCamera(gks)
      .fold(error => errorHandling(sender), bs => initPlugins(gks, bs))
  }

  private def initPlugins(gks: GlobalKillSwitch, bs: BroadCastRunnableGraph) = {
    pluginRegistry ! AdvancedPluginStart(gks, bs)
  }

  private def errorHandling(sender: ActorRef) = {
    sender ! Error("shit gonna change")
  }

  private def initCamera(killSwitch: GlobalKillSwitch): Either[Exception, BroadCastRunnableGraph] =
    broadCastMateralizer.create(killSwitch).future.onComplete {
      case Success(bs: BroadCastRunnableGraph) => Right(bs)
      case Failure(e)  => Left(e)
    }

}
