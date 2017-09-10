package sentinel.camera.utils.webcam

import java.awt.event.{WindowAdapter, WindowEvent}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, Frame, OpenCVFrameConverter}
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.{Camera, CameraFrame}
import sentinel.camera.framegrabber.FFmpegFrameGrabberBuilder
import sentinel.camera.utils.settings.PropertyFileSettingsLoader

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

  val settings = new PropertyFileSettingsLoader().load()
  val webcamSource = Camera.source(new FFmpegFrameGrabberBuilder(settings))
  // 30 fps => take a picture every 20 ms
  val tickingSource = Source.tick(1.second, 20.millisecond, 0)
  // Shared between flows in order to shutdown the whole graph
  val killSwitch = KillSwitches.shared("switch")

  def start(): Unit = {

    //    val webcamGraph = new CameraReaderGraph(webcamSource, tickingSource, killSwitch).createGraph
    //    val showImageGraph = new ShowImageGraph(webcamGraph, normalCanvas, killSwitch).createGraph
    // Obtain a Sink and Source which will publish and receive from the "bus" respectively

    def webcamGraph(): CameraSource =
      Source.fromGraph(GraphDSL.create() {
        implicit builder =>
          import GraphDSL.Implicits._
          val converter = new OpenCVFrameConverter.ToIplImage()
          val IplImageConverter: FlowShape[Frame, CameraFrame] = builder.add(Flow[Frame]
            .via(killSwitch.flow)
            .map(converter.convert)
            .map(CameraFrame(_)))

          val WebCam: Outlet[Frame] = builder.add(webcamSource).out
          val webcamStream = WebCam
            .via(killSwitch.flow)

          val stream = webcamStream ~> IplImageConverter
          SourceShape(stream.outlet)
      })

    //    val (cameraSink, cameraSource) =
    //    //      MergeHub.source[CameraFrame]
    //      webcamGraph
    //        .toMat(BroadcastHub.sink[CameraFrame](bufferSize = 256))(Keep.both)
    //        .run()

    // We create now a Flow that represents a publish-subscribe channel using the above
    // started stream as its "topic". We add two more features, external cancellation of
    // the registration and automatic cleanup for very slow subscribers.
    //    val busFlow: Flow[CameraFrame, CameraFrame, UniqueKillSwitch] =
    //    Flow.fromSinkAndSource(cameraSink, cameraSource)
    //      .joinMat(KillSwitches.singleBidi[CameraFrame, CameraFrame])(Keep.right)
    //      .backpressureTimeout(3.seconds)

    //    showImageGraph.run()

    //    showImageGraph.run()
    //    val busFlow: Flow[CameraFrame, CameraFrame, NotUsed] =
    //      Flow.fromSinkAndSource(cameraSink, cameraSource)
    //        .buffer(32, OverflowStrategy.backpressure)

    def source[A, M](normal: Source[A, M])(implicit fm: Materializer, system: ActorSystem): (Source[A, NotUsed], M) = {
      val (normalMat, hubSource) = normal.toMat(BroadcastHub.sink(bufferSize = 1))(Keep.both).run
      (hubSource, normalMat)
    }

    val (cameraSource, camMat) = source(webcamGraph())

    val converter = new OpenCVFrameConverter.ToIplImage()
    //    val switch: UniqueKillSwitch =
    cameraSource
      .zip(tickingSource)
      .map(_._1)
      //      .viaMat(busFlow)(Keep.left)
      .to(Sink.foreach(f => {
      logger.info("wat wat watt de fak")
      normalCanvas.showImage(converter.convert(f.image))
    }))
    //      .to(Sink.ignore)
    //      .run()

    //    cameraSource.to(Sink.foreach(f => normalCanvas.showImage(converter.convert(f.image)))).run()
    //    cameraSource.to(Sink.foreach(f => motionCanvas.showImage(converter.convert(f.image)))).run()


    //        .runForeach(f =>{
    //          normalCanvas.showImage(converter.convert(f.image))
    //        })
    //
    //        .throttle(1, 20.millisecond, 1, ThrottleMode.shaping)
    //        .map(f =>  normalCanvas.showImage(converter.convert(f.image)))
    //        .map(_ => println("processed"))
    //        .to(Sink.ignore)
    //        .run()

    normalCanvas.addWindowListener(new java.awt.event.WindowAdapter() {
      override def windowClosing(windowEvent: WindowEvent): Unit = {
        Await.ready(Future {
          //          switch.shutdown()
          killSwitch.shutdown()
          logger.debug("Shutdown.")
        }, 3.seconds)
      }
    })

    //    val showImageGraph = new ShowImageGraph(webcamGraph, normalCanvas, killSwitch).createGraph
    //
    //    val motionDetectGraph = RunnableGraph.fromGraph(GraphDSL.create(webcamGraph) {
    //      implicit builder =>
    //        (source) =>
    //          import GraphDSL.Implicits._
    //
    //          val mog = BackgroundSubtractorMOG2Factory()
    //          val substractor = new GaussianMixtureBasedBackgroundSubstractor(mog, 0.01)
    //          val motionDetectStage = new MotionDetectStage(substractor)
    //
    //          val MotionDetector: FlowShape[CameraFrame, CameraFrame] = builder.add(motionDetectStage)
    //
    //          val E: Inlet[Any] = builder.add(Sink.ignore).in
    //
    //          source.out ~> MotionDetector ~> E
    //
    //          ClosedShape
    //    })
    //
    //    val graph = RunnableGraph.fromGraph(GraphDSL.create() {
    //      implicit builder =>
    //        import GraphDSL.Implicits._
    //        // what is needed from here?
    //
    //        val mog = BackgroundSubtractorMOG2Factory()
    //        val substractor = new GaussianMixtureBasedBackgroundSubstractor(mog, 0.01)
    //        val motionDetectStage = new MotionDetectStage(substractor)
    //        val converter = new OpenCVFrameConverter.ToIplImage()
    //
    //        val IplImageConverter: FlowShape[Frame, CameraFrame] = builder.add(Flow[Frame]
    //          .via(killSwitch.flow)
    //          .map(converter.convert)
    //          .map(f => CameraFrame(f)))
    //
    //        val showFrame = Flow[CameraFrame].via(killSwitch.flow)
    //          .map(f => {
    //            val img = converter.convert(f.image)
    //            (f, img)
    //          })
    //          .map(f => {
    //            normalCanvas.showImage(f._2)
    //            f._1
    //          })
    //
    //        val showFrameAnotherBah = Flow[CameraFrame].via(killSwitch.flow)
    //          .map(f => {
    //            val img = converter.convert(f.image)
    //            (f, img)
    //          })
    //          .map(f => {
    //            motionCanvas.showImage(f._2)
    //            f._1
    //          })
    //
    //        val WebCam: Outlet[Frame] = builder.add(webcamSource).out
    //        val ShowNormalImage: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrame)
    //        val ShowMaskedImage: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrameAnotherBah)
    //        val MotionDetector: FlowShape[CameraFrame, CameraFrame] = builder.add(motionDetectStage)
    //        val bcast = builder.add(Broadcast[CameraFrame](2))
    //
    //        val E: Inlet[Any] = builder.add(Sink.ignore).in
    //        val F: Inlet[Any] = builder.add(Sink.ignore).in
    //
    //        val stream = WebCam
    //          .via(killSwitch.flow)
    //          .zip(tickingSource)
    //          .map(_._1)
    //
    //        stream ~> IplImageConverter ~> bcast.in
    //        bcast.out(0) ~> ShowNormalImage ~> E
    //        bcast.out(1) ~> MotionDetector ~> ShowMaskedImage ~> F
    //
    //        ClosedShape
    //    })


    //    showImageGraph.run()

  }
}