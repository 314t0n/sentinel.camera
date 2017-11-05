package sentinel.plugin.util

import akka.stream.ActorMaterializer
import akka.stream.KillSwitch
import akka.stream.KillSwitches
import akka.stream.SharedKillSwitch
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import sentinel.camera.camera.stage.ShowImageStage
import sentinel.plugin.Plugin
import sentinel.router.messages.PluginStart

import scala.util.Try

class ShowImage(canvas: CanvasFrame, converter: ToIplImage)(implicit mat: ActorMaterializer)
    extends Plugin
    with LazyLogging {

  var pluginKillSwitch: Option[SharedKillSwitch] = None

  override def start(ps: PluginStart): Unit =
    Try({

      pluginKillSwitch = Some(KillSwitches.shared("ShowImage"))

      logger.info("Starting image view")

      val (broadcast, killSwitch) = (ps.broadcast, ps.ks)
      val publisher               = broadcast.mat

      publisher
        .via(killSwitch.asInstanceOf[SharedKillSwitch].flow)
        .via(pluginKillSwitch.get.flow)
        .runWith(new ShowImageStage(canvas, converter))

    }) recover {
      case e: Exception => logger.error(e.getMessage, e)
    }

  override def stop(): Unit = pluginKillSwitch match {
    case Some(ks) => ks.shutdown()
    case None     => logger.error("shutdown")
  }
}
