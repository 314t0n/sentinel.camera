package sentinel

import java.awt.event.{WindowAdapter, WindowEvent}

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph, Source}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import sentinel.camera.camera.Camera
import sentinel.camera.camera.graph.CameraReaderGraph
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.stage.ShowImageStage
import sentinel.camera.framegrabber.FFmpegFrameGrabberBuilder
import sentinel.camera.motiondetector.bgsubtractor.{BackgroundSubtractorMOG2Factory, GaussianMixtureBasedBackgroundSubstractor}
import sentinel.camera.motiondetector.stage.BackgroundSubstractorStage
import sentinel.camera.utils.settings.PropertyFileSettingsLoader
import sentinel.graph.GraphFactory

import scala.concurrent.duration._

object Sandbox3 extends App with LazyLogging {

  def addCloseEvent(canvas: CanvasFrame): Unit =
    canvas.addWindowListener(new WindowAdapter() {
      override def windowClosing(windowEvent: WindowEvent): Unit = {
        logger.debug("Stopping GUI")
      }
    })

  private val timeout = 5000
  //  implicit val ex = scala.concurrent.ExecutionContext.global
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(
        initialSize = 1,
        maxSize = 1))

  val settings = new PropertyFileSettingsLoader().load()

  val webcamSource = Camera.source(new FFmpegFrameGrabberBuilder(settings))
  // 30 fps => take a picture every 20 ms
  val tickingSource = Source.tick(1.second, 20.millisecond, 0)
  // Shared between flows in order to shutdown the whole graph
  var killSwitch = KillSwitches.shared("switch")

  val motionCanvas = new CanvasFrame("Masked")
  val normalCanvas = new CanvasFrame("Webcam")

  logger.info("Starting GUI")
  //  Set Canvas frame to close on exit
  motionCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
  normalCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
  addCloseEvent(normalCanvas)

  // ------------------------

  val producer = new CameraReaderGraph(webcamSource, tickingSource, killSwitch).createGraph()

  val publisher = new BroadcastingGraph(producer).createGraph().run()
  val converter = new OpenCVFrameConverter.ToIplImage()
  // ------------------------

  publisher
    .via(killSwitch.flow)
    .runWith(new ShowImageStage(normalCanvas, converter))

  val mog = BackgroundSubtractorMOG2Factory()
  val substractor = new GaussianMixtureBasedBackgroundSubstractor(mog, 0.01)

  publisher
    .via(killSwitch.flow)
    .via(new BackgroundSubstractorStage(substractor))
    .runWith(new ShowImageStage(motionCanvas, converter))

  // ------------------------
  logger.info("Starting 1st time")
  Thread.sleep(5000)

  logger.info("Terminating 1st time")
  killSwitch.shutdown()

  Thread.sleep(5000)

  killSwitch = KillSwitches.shared("switch")
  val killSwitch2 = KillSwitches.shared("switch")

  val producer2 = new CameraReaderGraph(webcamSource, tickingSource, killSwitch).createGraph()
  val publisher2 = new BroadcastingGraph(producer2).createGraph().run()

  publisher2
    .via(killSwitch.flow)
    .runWith(new ShowImageStage(normalCanvas, converter))
  Thread.sleep(1000)
  publisher2
    .via(killSwitch.flow)
    .via(killSwitch2.flow)
    .via(new BackgroundSubstractorStage(substractor))
    .runWith(new ShowImageStage(motionCanvas, converter))
  logger.info("Starting 2nd time")
  Thread.sleep(8000)

  logger.info("Terminating 2nd time")
  killSwitch2.shutdown()

  Thread.sleep(4000)
  killSwitch.shutdown()
  system.terminate()

  // ------------------------

  class BroadcastingGraph(source: CameraSource)
    extends GraphFactory[RunnableGraph[CameraSource]]
      with LazyLogging {
    override def createGraph(): RunnableGraph[CameraSource] =
      source.toMat(BroadcastHub.sink(bufferSize = 1))(Keep.right)
  }

}

