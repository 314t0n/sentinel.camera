package sentinel.camera.motiondetector.plugin

import akka.actor.ActorRef
import akka.dispatch.Envelope
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.ActorMaterializer
import akka.stream.KillSwitches
import akka.stream.SharedKillSwitch
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv._
import sentinel.camera.camera.CameraFrame
import sentinel.camera.camera.MotionDetectFrame
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import sentinel.camera.motiondetector.stage.BackgroundSubstractorStage
import sentinel.plugin.Plugin
import sentinel.router.messages.AdvancedPluginStart

import scala.util.Try

class MotionDetectorPlugin(canvas: CanvasFrame,
                           iplImageConverter: ToIplImage,
                           matConverter: ToMat,
                           backgroundSubstractor: BackgroundSubstractor,
                           name: String = "",
                           notifier: ActorRef)(implicit mat: ActorMaterializer)
    extends Plugin
    with LazyLogging {

  val structuringElementSize                     = new Size(4, 4)
  var pluginKillSwitch: Option[SharedKillSwitch] = None

  /**
    * @see https://docs.opencv.org/2.4.13.4/doc/tutorials/imgproc/erosion_dilatation/erosion_dilatation.html
    */
  private def erosionAndDilation(backgroundSubstractedFrame: MotionDetectFrame) = {
    val structuringElement = getStructuringElement(MORPH_RECT, structuringElementSize)
    val frameAsMat         = toMat(backgroundSubstractedFrame.originalFrame.image)
    morphologyEx(frameAsMat, frameAsMat, MORPH_OPEN, structuringElement)
    frameAsMat.release()
    backgroundSubstractedFrame
  }

  private def toMat(image: IplImage) = matConverter.convert(iplImageConverter.convert(image))

  override def start(ps: AdvancedPluginStart): Unit =
    Try({

      pluginKillSwitch = Some(KillSwitches.shared("BackgroundSubstractor"))
      val (broadcast, killSwitch) = (ps.broadcast, ps.ks.sharedKillSwitch)

      broadcast.mat
        .via(killSwitch.flow)
        .via(pluginKillSwitch.get.flow)
        .via(new BackgroundSubstractorStage(backgroundSubstractor))
        .async.via(Flow[MotionDetectFrame].map(erosionAndDilation))
        .async.via(Flow[MotionDetectFrame].filter(reachedThreshold))
        .async.via(Flow[MotionDetectFrame].map(_.originalFrame))
        .async.runWith(Sink.foreach(sendNotification))
      //        .runWith(new ShowImageStage(canvas, iplImageConverter, name))

    }) recover {
      case e: Exception => logger.error(e.getMessage, e)
    }

  private def sendNotification(f: CameraFrame) = notifier ! f

  private def reachedThreshold(f: MotionDetectFrame): Boolean = cvCountNonZero(f.masked) > 5000

  override def stop(): Unit = pluginKillSwitch match {
    case Some(ks) => ks.shutdown()
    case None     => logger.error("shutdown")
  }
}
