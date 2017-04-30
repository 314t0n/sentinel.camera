package sentinel.camera.utils

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import org.bytedeco.javacv.{CanvasFrame, Frame, OpenCVFrameConverter}
import sentinel.camera.motiondetect.MotionDetectStage
import sentinel.camera.motiondetect.bgsubtractor.GaussianMixtureBasedBackgroundSubstractor
import sentinel.camera.webcam.{CameraFrame, WebCamera}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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

  val imageDimensions = Dimensions(width = 640, height = 320)
  val webcamSource = WebCamera.source(deviceId = 0, dimensions = imageDimensions, framePerSec = 30)
  // 30 fps => take a picture every 20 ms
  val tickingSource = Source.tick(1.second, 20.millisecond, 0)
  // Shared between flows in order to shutdown the whole graph
  val killSwitch = KillSwitches.shared("switch")

  val graph = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder =>
      import GraphDSL.Implicits._
      // TODO naming
      val motionDetectStage = new MotionDetectStage()
      val converter = new OpenCVFrameConverter.ToIplImage()
      val substractor = new GaussianMixtureBasedBackgroundSubstractor()
      //      val MotionDetect: FlowShape[IplImage, IplImage] = builder.add(new MotionDetectStage())
      val ToIplImage: FlowShape[Frame, CameraFrame] = builder.add(Flow[Frame]
        .via(killSwitch.flow)
        .map(converter.convert)
        .map(f => CameraFrame(f)))

      val motionDetectFlow = Flow[CameraFrame].via(killSwitch.flow)
        .buffer(10, OverflowStrategy.backpressure)
        .map(substractor.substractBackground)
      //        .map(f => edgeFilter.filter(f))
      //        .map(f => grayFilter.filter(f))
      //
      val showFrame = Flow[CameraFrame].via(killSwitch.flow)
        //        .map(f => f.image)
        .map(f => {
        val img = converter.convert(f.image)
        (f, img)
      })
        .map(f => {
          canvas.showImage(f._2)
          f._1
        })

      val closer = Flow[CameraFrame].map(f => {
        try {
          f.image.release()
        } catch {
          case e: Exception => {
            println(e)
          }
        }
      })

      val WebCam: Outlet[Frame] = builder.add(webcamSource).out
      val ShowFrame: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrame)
      //      val MotionDetector: FlowShape[CameraFrame, CameraFrame] = builder.add(motionDetectFlow)

      val MotionDetector: FlowShape[CameraFrame, CameraFrame] = builder.add(motionDetectStage)

      //      val SourceClose1: FlowShape[CameraFrame, Unit] = builder.add(closer)
      //      val SourceClose2: FlowShape[CameraFrame, Unit] = builder.add(closer)
      //      val bcast = builder.add(Broadcast[CameraFrame](2))
      //      val merge = builder.add(Zip[CameraFrame, CameraFrame])
      val E: Inlet[Any] = builder.add(Sink.ignore).in

      val stream = WebCam
        .via(killSwitch.flow)
        .zip(tickingSource)
        .map(_._1)

      //            stream ~> ShowFrame ~> E
      //            stream ~> ToIplImage ~> MotionDetector ~> ShowFrame ~> SourceClose1 ~> E
      stream ~> ToIplImage ~> MotionDetector ~> ShowFrame ~> E
      //      stream ~> ToIplImage ~> bcast.in
      //      bcast.out(0) ~> SourceClose1 ~> E
      //      bcast.out(1) ~> MotionDetector ~> E

      ClosedShape
  })

  canvas.addWindowListener(new java.awt.event.WindowAdapter() {
    override def windowClosing(windowEvent: java.awt.event.WindowEvent): Unit = {
      println(LocalDateTime.now + " closing window")
      Await.ready(Future {
        killSwitch.shutdown()
        println(LocalDateTime.now + " shutdown")
      }, 3.seconds)
    }
  })
  graph.run()
}
