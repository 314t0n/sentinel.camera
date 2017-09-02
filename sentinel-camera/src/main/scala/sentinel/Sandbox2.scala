package sentinel

import java.awt.event.{WindowAdapter, WindowEvent}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.CanvasFrame
import sentinel.Sandbox.{killSwitch, tickingSource}
import sentinel.camera.framegrabber.FFmpegFrameGrabberBuilder
import sentinel.camera.utils.settings.PropertyFileSettingsLoader
import sentinel.camera.webcam.WebCamera
import sentinel.camera.webcam.graph.CameraReaderGraph.CameraSource
import sentinel.camera.webcam.graph.{CameraReaderGraph, ShowImageGraph}
import sentinel.graph.GraphFactory

import scala.concurrent.duration._

object Sandbox2 extends App with LazyLogging {

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
  val webcamSource = WebCamera.source(new FFmpegFrameGrabberBuilder(settings))
  // 30 fps => take a picture every 20 ms
  val tickingSource = Source.tick(1.second, 20.millisecond, 0)
  // Shared between flows in order to shutdown the whole graph
  val killSwitch = KillSwitches.shared("switch")

  val motionCanvas = new CanvasFrame("Masked")
  val normalCanvas = new CanvasFrame("Webcam")

  logger.info("Starting GUI")
  //  Set Canvas frame to close on exit
  motionCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
  normalCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
  addCloseEvent(normalCanvas)

  val producer = new CameraReaderGraph(webcamSource, tickingSource, killSwitch).createGraph()

  val fromProducer = new BroadcastingGraph(producer).createGraph().run()

  val processorA = new ShowImageGraph(fromProducer, normalCanvas, killSwitch).createGraph

  processorA.run()

  class BroadcastingGraph(source: CameraSource)
    extends GraphFactory[RunnableGraph[CameraSource]]
      with LazyLogging {
    override def createGraph(): RunnableGraph[CameraSource] =
      source.toMat(BroadcastHub.sink(bufferSize = 1))(Keep.right)
  }

}

