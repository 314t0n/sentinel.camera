package sentinel

import java.awt.event.WindowAdapter
import javax.swing.JFrame.EXIT_ON_CLOSE

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter
import sentinel.app.Orchestator
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import sentinel.camera.motiondetector.bgsubtractor.GaussianMixtureBasedBackgroundSubstractor
import sentinel.camera.motiondetector.plugin.MotionDetectorPlugin
import sentinel.plugin.util.ShowImage
import sentinel.system.module.ModuleInjector

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

object POCApp extends App with LazyLogging {

  System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0")
  System.setProperty("org.bytedeco.javacpp.maxbytes", "0")
//  System.setProperty("org.bytedeco.javacpp.logger.debug", "true")

  logger.info(s"Sentinel camera view start up ...")

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(initialSize = 1, maxSize = 1))

  private implicit val executionContext =
    materializer.system.dispatchers.defaultGlobalDispatcher
  private val modules               = new ModuleInjector(system, materializer)
  private val orchestator           = modules.injector.getInstance(classOf[Orchestator])
  private val backgroundSubstractor = modules.injector.getInstance(classOf[GaussianMixtureBasedBackgroundSubstractor])

  lazy val shutdown: Unit = {
    logger.info(s"Sentinel camera view shutdown.")
    stopStreaming(orchestator)
    materializer.shutdown()
  }

  val converter  = () => new OpenCVFrameConverter.ToIplImage()
  val converter2 = () => new OpenCVFrameConverter.ToMat()
  val canvas     = createCanvas(shutdown)
  val canvasMD   = createCanvas(shutdown)

  startStreaming(orchestator)

  sleep(6000) // nice :(

  val showImagePlugin = new ShowImage(canvas, converter())(materializer)
  orchestator.addPlugin(showImagePlugin)
  val motionDetect = new MotionDetectorPlugin(canvasMD, converter(), converter2(), backgroundSubstractor)(materializer)
  orchestator.addPlugin(motionDetect)
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
      Thread.sleep(ms - 10)
    }, ms millisecond)
  }
}
