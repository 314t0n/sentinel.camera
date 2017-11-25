package sentinel

import java.awt.event.WindowAdapter
import javax.swing.JFrame.EXIT_ON_CLOSE

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import sentinel.app.Orchestator
import sentinel.plugin.util.ShowImage
import sentinel.system.module.ModuleInjector

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object POCApp extends App with LazyLogging {

  logger.info(s"Sentinel camera view start up ...")

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(initialSize = 1, maxSize = 1))

  private implicit val executionContext =
    materializer.system.dispatchers.defaultGlobalDispatcher
  private val modules = new ModuleInjector(system, materializer)
  private val buncher = modules.injector.getInstance(classOf[Orchestator])

  lazy val shutdown: Unit = {
    logger.info(s"Sentinel camera view shutdown.")
    stopStreaming(buncher)
    materializer.shutdown()
  }

  val converter = () => new OpenCVFrameConverter.ToIplImage()
  val canvas    = createCanvas(shutdown)

  startStreaming(buncher)

  sleep(4000)

  val showImagePlugin = new ShowImage(canvas, converter())(materializer)
  buncher.addPlugin(showImagePlugin)
  sys.addShutdownHook(shutdown)

  private def startStreaming(buncher: Orchestator) = {
    val start = buncher.start()

    logger.info("Video streaming started.")
  }

  private def stopStreaming(buncher: Orchestator) = {
    logger.info("Shutdown video stream ...")
    val stop = buncher.stop()

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

  def sleep(ms: Int) = {
    Await.ready(Future {
      Thread.sleep(ms - 1)
    }, ms millisecond)
  }
}
