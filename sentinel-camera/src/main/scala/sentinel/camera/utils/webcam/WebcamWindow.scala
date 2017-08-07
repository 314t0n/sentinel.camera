package sentinel.camera.utils.webcam

import java.awt.event.{WindowAdapter, WindowEvent}

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, Frame, OpenCVFrameConverter}
import sentinel.camera.framegrabber.FFmpegFrameGrabberBuilder
import sentinel.camera.motiondetector.MotionDetectStage
import sentinel.camera.motiondetector.bgsubtractor.{BackgroundSubtractorMOG2Factory, GaussianMixtureBasedBackgroundSubstractor}
import sentinel.camera.utils.settings.PropertyBasedSettingsLoader
import sentinel.camera.webcam.{CameraFrame, WebCamera}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class WebcamWindow extends LazyLogging {

  def start(): Unit = {
    logger.info("Starting GUI")
    val motionCanvas = new CanvasFrame("Masked")
    val normalCanvas = new CanvasFrame("Webcam")
    //  Set Canvas frame to close on exit
    motionCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
    normalCanvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE)
    addCloseEvent(normalCanvas)
    new GraphSometin(normalCanvas, motionCanvas).start()
  }

  def addCloseEvent(canvas: CanvasFrame): Unit =
    canvas.addWindowListener(new WindowAdapter() {
      override def windowClosing(windowEvent: WindowEvent): Unit = {
        logger.debug("Stopping GUI")
      }
    })
}

class GraphSometin(normalCanvas: CanvasFrame,
                   motionCanvas: CanvasFrame)
  extends LazyLogging {

  logger.debug("Start up")

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(
        initialSize = 1,
        maxSize = 1))

  import system.dispatcher

  // todo move these to settings
  val imageDimensions = Dimensions(width = 640, height = 320)

  val settings = new PropertyBasedSettingsLoader().load()
  val webcamSource = WebCamera.source(new FFmpegFrameGrabberBuilder(settings))
  // 30 fps => take a picture every 20 ms
  val tickingSource = Source.tick(1.second, 20.millisecond, 0)
  // Shared between flows in order to shutdown the whole graph
  val killSwitch = KillSwitches.shared("switch")

  def start(): Unit = {

    val webcamGraph = Source.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        val converter = new OpenCVFrameConverter.ToIplImage()

        val IplImageConverter: FlowShape[Frame, CameraFrame] = builder.add(Flow[Frame]
          .via(killSwitch.flow)
          .map(converter.convert)
          .map(f => CameraFrame(f)))

        val WebCam: Outlet[Frame] = builder.add(webcamSource).out

        val webcamStream = WebCam
          .via(killSwitch.flow)
          .zip(tickingSource)
          .map(_._1)

        val stream = webcamStream ~> IplImageConverter

        SourceShape(stream.outlet)
    })

    val showImageGraph = RunnableGraph.fromGraph(GraphDSL.create(webcamGraph) {
      implicit builder =>
        (source) =>
          import GraphDSL.Implicits._

          val converter = new OpenCVFrameConverter.ToIplImage()
          val showFrame = Flow[CameraFrame].via(killSwitch.flow)
            .map(f => (f, converter.convert(f.image))) // strange stuff, please simplify
            .map(f => {
              normalCanvas.showImage(f._2)
              f._1
            })

          val ShowNormalImage: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrame)
          val Ignore: Inlet[Any] = builder.add(Sink.ignore).in

          source.out ~> ShowNormalImage ~> Ignore

          ClosedShape
    })

    val graph = RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._
        // what is needed from here?

        val mog = BackgroundSubtractorMOG2Factory()
        val substractor = new GaussianMixtureBasedBackgroundSubstractor(mog, 0.01)
        val motionDetectStage = new MotionDetectStage(substractor)
        val converter = new OpenCVFrameConverter.ToIplImage()

        val IplImageConverter: FlowShape[Frame, CameraFrame] = builder.add(Flow[Frame]
          .via(killSwitch.flow)
          .map(converter.convert)
          .map(f => CameraFrame(f)))

        val showFrame = Flow[CameraFrame].via(killSwitch.flow)
          .map(f => {
            val img = converter.convert(f.image)
            (f, img)
          })
          .map(f => {
            normalCanvas.showImage(f._2)
            f._1
          })

        val showFrameAnotherBah = Flow[CameraFrame].via(killSwitch.flow)
          .map(f => {
            val img = converter.convert(f.image)
            (f, img)
          })
          .map(f => {
            motionCanvas.showImage(f._2)
            f._1
          })

        val WebCam: Outlet[Frame] = builder.add(webcamSource).out
        val ShowNormalImage: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrame)
        val ShowMaskedImage: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrameAnotherBah)
        val MotionDetector: FlowShape[CameraFrame, CameraFrame] = builder.add(motionDetectStage)
        val bcast = builder.add(Broadcast[CameraFrame](2))

        val E: Inlet[Any] = builder.add(Sink.ignore).in
        val F: Inlet[Any] = builder.add(Sink.ignore).in

        val stream = WebCam
          .via(killSwitch.flow)
          .zip(tickingSource)
          .map(_._1)

        stream ~> IplImageConverter ~> bcast.in
        bcast.out(0) ~> ShowNormalImage ~> E
        bcast.out(1) ~> MotionDetector ~> ShowMaskedImage ~> F

        ClosedShape
    })

    normalCanvas.addWindowListener(new java.awt.event.WindowAdapter() {
      override def windowClosing(windowEvent: WindowEvent): Unit = {
        Await.ready(Future {
          killSwitch.shutdown()
          logger.debug("Shutdown.")
        }, 3.seconds)
      }
    })

    showImageGraph.run()

  }
}