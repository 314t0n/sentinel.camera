package sentinel.camera.motiondetector.plugin

import akka.stream.ActorMaterializer
import akka.stream.KillSwitches
import akka.stream.SharedKillSwitch
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import sentinel.camera.camera.CameraFrame
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import sentinel.camera.motiondetector.stage.BackgroundSubstractorStage
import sentinel.plugin.Plugin
import sentinel.router.messages.AdvancedPluginStart
import org.bytedeco.javacpp._
import org.bytedeco.javacv._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import sentinel.camera.camera.stage.ShowImageStage

import scala.util.Try

class MotionDetectorPlugin(canvas: CanvasFrame,
                           iplImageConverter: ToIplImage,
                           matConverter: ToMat,
                           backgroundSubstractor: BackgroundSubstractor)(implicit mat: ActorMaterializer)
    extends Plugin
    with LazyLogging {

  var pluginKillSwitch: Option[SharedKillSwitch] = None
// fatal error
  private def createGrayFrame(frame: CameraFrame) = {
    val grayImage = cvCreateImage(frame.image.cvSize(), IPL_DEPTH_8U, 1)
    cvCvtColor(frame.image, grayImage, CV_RGB2GRAY)
    grayImage
    frame.copy(grayImage)
  }

  private def filter(frame: CameraFrame) = {
    val structuringElement = getStructuringElement(MORPH_RECT, new Size(5, 5))
    val image = cvCreateImage(frame.image.cvSize(), IPL_DEPTH_8U, 1)
    val mat1 = toMat(frame.image)
    val mat2 = toMat(image)
    morphologyEx(mat1, mat2, MORPH_OPEN, structuringElement)
    mat1.release()
    mat2.release()
    frame.copy(image)
  }

  private def toMat(image: IplImage) = {
    matConverter.convert(iplImageConverter.convert(image))
  }

  override def start(ps: AdvancedPluginStart): Unit =
    Try({

      pluginKillSwitch = Some(KillSwitches.shared("BackgroundSubstractor"))
      val (broadcast, killSwitch) = (ps.broadcast, ps.ks.sharedKillSwitch)

      broadcast.mat
//        .via(killSwitch.flow)
        .via(pluginKillSwitch.get.flow)
        .via(new BackgroundSubstractorStage(backgroundSubstractor))
        .via(Flow[CameraFrame].map(filter))
        .runWith(new ShowImageStage(canvas, iplImageConverter))

    }) recover {
      case e: Exception => logger.error(e.getMessage, e)
    }

  override def stop(): Unit = pluginKillSwitch match {
    case Some(ks) => ks.shutdown()
    case None     => logger.error("shutdown")
  }
}
