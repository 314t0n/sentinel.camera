package sentinel.camera.utils

import javax.swing.JOptionPane

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.actor.ActorPublisherMessage.Cancel
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import org.bytedeco.javacv.{CanvasFrame, Frame}
import sentinel.camera.utils.transform.{Flip, MediaConversion}
import sentinel.camera.webcam.WebCamera

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object WebcamWindow extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(
        initialSize = 1,
        maxSize = 1))

  import system.dispatcher

  val canvas = new CanvasFrame("Webcam")
  //  Set Canvas frame to close on exit
  canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)

  val imageDimensions = Dimensions(width = 640, height = 480)
  val webcamSource = WebCamera.source(deviceId = 0, dimensions = imageDimensions, framePerSec = 30)

  val killSwitch = KillSwitches.shared("switch")

  val graph = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder =>
      import GraphDSL.Implicits._

      val showFrame = Flow[Frame].map(canvas.showImage)

      val WebCam: Outlet[Frame] = builder.add(webcamSource).out
      val ShowFrame: FlowShape[Frame, Unit] = builder.add(showFrame)
      val E: Inlet[Any] = builder.add(Sink.ignore).in

      WebCam.via(killSwitch.flow) ~> ShowFrame ~> E

      ClosedShape
  })

  graph.run()

  canvas.addWindowListener(new java.awt.event.WindowAdapter() {
    override def windowClosing(windowEvent: java.awt.event.WindowEvent): Unit = {
      println("Closing window")
      println("killSwitch")
      killSwitch.shutdown()
    }
  })
}
