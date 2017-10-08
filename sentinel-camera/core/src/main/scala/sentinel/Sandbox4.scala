package sentinel

import java.awt.event.{WindowAdapter, WindowEvent}

import akka.actor.{ActorSystem, FSM, Props}
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph, Source}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import sentinel.Sandbox3.logger
import sentinel.camera.camera.Camera
import sentinel.camera.camera.graph.CameraReaderGraph
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.stage.ShowImageStage
import sentinel.camera.framegrabber.FFmpegFrameGrabberBuilder
import sentinel.camera.utils.settings.PropertyFileSettingsLoader
import sentinel.graph.GraphFactory

import scala.concurrent.duration._

object Sandbox4 extends App with LazyLogging {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(
        initialSize = 1,
        maxSize = 1))

  class Factory() {

    def create(killSwitch: SharedKillSwitch) = {

      def addCloseEvent(canvas: CanvasFrame): Unit =
        canvas.addWindowListener(new WindowAdapter() {
          override def windowClosing(windowEvent: WindowEvent): Unit = {
            logger.debug("Stopping GUI")
          }
        })

      val settings = new PropertyFileSettingsLoader().load()

      val webcamSource = Camera.source(new FFmpegFrameGrabberBuilder(settings))
      // 30 fps => take a picture every 20 ms
      val tickingSource = Source.tick(1.second, 20.millisecond, 0)
      // Shared between flows in order to shutdown the whole graph


      val motionCanvas = new CanvasFrame("Masked")
      val normalCanvas = new CanvasFrame("Webcam")

      logger.info("Starting GUI")
      //  Set Canvas frame to close on exit
      motionCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
      normalCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
      addCloseEvent(normalCanvas)

      val producer = new CameraReaderGraph(webcamSource, tickingSource, killSwitch).createGraph()

      val publisher = new BroadcastingGraph(producer).createGraph().run()

      val converter = new OpenCVFrameConverter.ToIplImage()

      publisher
        .via(killSwitch.flow)
        .runWith(new ShowImageStage(normalCanvas, converter))

      publisher
    }
  }

  class BroadcastingGraph(source: CameraSource)
    extends GraphFactory[RunnableGraph[CameraSource]]
      with LazyLogging {
    override def createGraph(): RunnableGraph[CameraSource] =
      source.toMat(BroadcastHub.sink(bufferSize = 1))(Keep.right)
  }


  sealed trait State

  case object Idle extends State

  case object Active extends State

  sealed trait Command

  case object Uninitialized extends Command

  case class Start(ks: SharedKillSwitch) extends Command

  case object Stop extends Command

  case object Restart extends Command

  class Buncher(factory: Factory) extends FSM[State, Command] {

    startWith(Idle, Stop)

    when(Idle) {
      case Event(Start(ks), _) =>
        goto(Active) using Start(ks)
    }

    onTransition {
      case Active -> Idle =>
        println(stateData)
        stateData match {
          case Start(ks) => println("killswitch shutdown")
          case _ => println("Active -> Idle")
        }
      case Idle -> Active =>
        println(stateData)
        stateData match {
          case Stop => {
//            val publisher = factory.create(ks)
          }
          case _ => println("Idle -> Active")
        }
    }

    when(Active, stateTimeout = 1 second) {
      case Event(Stop, _) =>
        goto(Idle) using Uninitialized
    }

    initialize()
  }

  val buncher = system.actorOf(Props(new Buncher(new Factory())), "buncher")
  val killSwitch = KillSwitches.shared("switch")

  buncher ! Start(killSwitch)
  Thread.sleep(10000)
  buncher ! Stop
  Thread.sleep(5000)
  buncher ! Start(killSwitch)
  Thread.sleep(10000)
  buncher ! Stop
  Thread.sleep(1000)
  system.terminate()

}

