package sentinel.camera.motiondetector.bgsubtractor.plugin

import akka.stream.{ActorMaterializer, KillSwitches, SharedKillSwitch}
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.stage.ShowImageStage
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import sentinel.camera.motiondetector.stage.BackgroundSubstractorStage
import sentinel.plugin.Plugin
import sentinel.router.messages.{AdvancedPluginStart, PluginStart}

import scala.util.Try

class BackgroundSubstractorPlugin(backgroundSubstractor: BackgroundSubstractor) (implicit mat: ActorMaterializer)
  extends Plugin
    with LazyLogging {
  var pluginKillSwitch: Option[SharedKillSwitch] = None

  override def start(ps: AdvancedPluginStart): Unit =
    Try({

      pluginKillSwitch = Some(KillSwitches.shared("ShowImage"))

      logger.info("Starting image view")

      val (broadcast, killSwitch) = (ps.broadcast, ps.ks.sharedKillSwitch)

      logger.info(broadcast.toString)

      val publisher               = broadcast.mat

      publisher
        .via(killSwitch.flow)
        .via(pluginKillSwitch.get.flow)
        .via(new BackgroundSubstractorStage(backgroundSubstractor))

    }) recover {
      case e: Exception => logger.error(e.getMessage, e)
    }

  override def stop(): Unit = pluginKillSwitch match {
    case Some(ks) => ks.shutdown()
    case None     => logger.error("shutdown")
  }
}
