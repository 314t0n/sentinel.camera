package sentinel.camera.motiondetector.plugin

import akka.actor.ActorRef
import akka.stream.ActorMaterializer
import akka.stream.KillSwitches
import akka.stream.OverflowStrategy
import akka.stream.SharedKillSwitch
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacpp.opencv_core
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

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.concurrent.forkjoin.ForkJoinPool
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.bytedeco.javacpp.opencv_imgproc.putText

class StreamerPlugin(notifier: ActorRef)(implicit mat: ActorMaterializer) extends Plugin with LazyLogging {

  implicit val ec = ExecutionContext.fromExecutor(new ForkJoinPool(2))

  var pluginKillSwitch: Option[SharedKillSwitch] = None

  import org.bytedeco.javacv.OpenCVFrameConverter

  val converterToMat = new OpenCVFrameConverter.ToMat()
  val converterToIpl = new OpenCVFrameConverter.ToIplImage()

  override def start(ps: AdvancedPluginStart): Unit =
    Try({

      pluginKillSwitch = Some(KillSwitches.shared("Streamer"))
      val (broadcast, killSwitch) = (ps.broadcast, ps.ks.sharedKillSwitch)
      val bufferSize              = 100
      val buffer =
        Flow[CameraFrame].scan[Seq[CameraFrame]](Seq.empty[CameraFrame]) { (seq, i) =>
          if (seq.size < bufferSize) {
            println("scenario A")
            seq :+ i
          } else {
            println("scenario B")
            Seq(i)
          }
        }

      broadcast.mat
        .via(killSwitch.flow)
        .via(pluginKillSwitch.get.flow)
        //        .buffer(30, OverflowStrategy.dropNew)
        .map(f => {
          println(" new frame " + f.date)
          val frame: Mat = converterToMat.convert(converterToMat.convert(f.image))
          val box_text   = f.date.toString
          val point      = new opencv_core.Point(50, 20)
          val scalar     = new opencv_core.Scalar(0, 255, 0, 2.0)
          val font       = FONT_HERSHEY_PLAIN
          putText(frame, box_text, point, font, 1.0, scalar)

          CameraFrame(converterToIpl.convert(converterToIpl.convert(frame)), f.date)
        })
//        .map(f => Seq(f))
        .groupedWithin(5, 1000 millis)
        //        .buffer(bufferSize, OverflowStrategy.fail)
//        .via(Flow[CameraFrame].grouped(bufferSize))
//        .async
        .runWith(Sink.foreach(sendNotificationBatch))
//        .runWith(Sink.foreach(sendNotificationBatch))

    }) recover {
      case e: Exception => logger.error(e.getMessage, e)
    }

  private def sendNotificationBatch(f: Seq[CameraFrame]) = {
    f.foreach(frame => println(s"send ${frame.date}"))
    println("called " + f.size)
    notifier ! f
  }

  private def sendNotification(f: CameraFrame) = {
    notifier ! f
  }

  override def stop(): Unit = pluginKillSwitch match {
    case Some(ks) => ks.shutdown()
    case None     => logger.error("shutdown")
  }
}
