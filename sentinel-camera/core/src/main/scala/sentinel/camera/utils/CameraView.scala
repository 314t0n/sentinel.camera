package sentinel.camera.utils

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import javax.swing.JButton
import javax.swing.JFrame.EXIT_ON_CLOSE

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.stream.SharedKillSwitch
import com.google.inject.Key
import com.google.inject.name.Names
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import sentinel.app.Buncher
import sentinel.camera.camera.stage.ShowImageStage
import sentinel.plugin.util.ShowImage
import sentinel.router.RouterFSM
import sentinel.router.messages.Error
import sentinel.router.messages.PluginStart
import sentinel.router.messages.SourceInit
import sentinel.router.messages.Start
import sentinel.system.module.ModuleInjector

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object CameraView extends App with LazyLogging {

  logger.info(s"Sentinel camera view start up ...")

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(initialSize = 1, maxSize = 1))
  private implicit val executionContext =
    materializer.system.dispatchers.defaultGlobalDispatcher
  private val modules = new ModuleInjector(system, materializer)
  private val buncher = modules.injector.getInstance(classOf[Buncher])

  lazy val shutdown: Unit = {
    logger.info(s"Sentinel camera view shutdown.")
    stopStreaming(buncher)
    materializer.shutdown()
  }

  val converter = new OpenCVFrameConverter.ToIplImage()
  val canvas = createCanvas(shutdown)
  val canvas2 = createCanvas(shutdown)
  val canvas3 = createCanvas(shutdown)

  startStreaming(buncher)

  val showImagePlugin = new ShowImage(canvas, converter)(materializer)
  val showImagePlugin2 = new ShowImage(canvas2, converter)(materializer)
  val showImagePlugin3 = new ShowImage(canvas3, converter)(materializer)

  buncher.addPlugin(showImagePlugin)
  buncher.addPlugin(showImagePlugin2)
  buncher.addPlugin(showImagePlugin3)

  sys.addShutdownHook(shutdown)

  private def startStreaming(buncher: Buncher) = {
    val start = buncher.start()

    start.future.onComplete {
      case Success(msg) => logger.info(s"Started. Last message was: $msg")
      case Failure(e)   => logger.error(e.getMessage, e)
    }

    Await.ready(start.future, 5 seconds)

    logger.info("Video streaming started.")
  }

  private def stopStreaming(buncher: Buncher) = {
    logger.info("Shutdown video stream ...")
    val stop = buncher.stop()

    stop.future.onComplete {
      case Success(msg) => logger.info(s"Stopped. Last message was: $msg")
      case Failure(e)   => logger.error(e.getMessage, e)
    }

    Await.ready(stop.future, 5 seconds)

    logger.info("Video streaming stopped.")
  }

  private def createCanvas(shutdown: => Unit): CanvasFrame = {
    val canvas = new CanvasFrame("Sentinel Camera View Util")
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)
    canvas.addWindowListener(new WindowAdapter() {
      override def windowClosing(windowEvent: java.awt.event.WindowEvent): Unit = {
        logger.debug("Canvas close")
        shutdown
      }
    })
    canvas
  }

  @deprecated
  class ShowImageActor(canvas: CanvasFrame, converter: ToIplImage) extends Actor with ActorLogging with LazyLogging {
    override def receive: Receive = {
      case PluginStart(killSwitch, broadcast) =>
        val requestor = sender()
        Try({

          val publisher = broadcast.graph.run()
          publisher
            .via(killSwitch.asInstanceOf[SharedKillSwitch].flow)
            .runWith(new ShowImageStage(canvas, converter))

        }) recover {
          case e: Exception => requestor ! Error(e.getMessage)
        }
    }
  }
}
