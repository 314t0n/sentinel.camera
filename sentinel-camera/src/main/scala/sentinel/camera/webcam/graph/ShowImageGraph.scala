package sentinel.camera.webcam.graph

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, MergeHub, RunnableGraph, Sink}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import sentinel.camera.webcam.CameraFrame
import sentinel.camera.webcam.graph.CameraReaderGraph.CameraSource
import sentinel.graph.GraphFactory

class ShowImageShape(canvas: CanvasFrame) extends GraphStage[SinkShape[CameraFrame]] {

  private val in = Inlet[CameraFrame]("ShowImage.in")
  private val converter = new OpenCVFrameConverter.ToIplImage()

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val elem: CameraFrame = grab(in)
        canvas.showImage(converter.convert(elem.image))
        pull(in)
      }
    })

    override def preStart(): Unit = pull(in)
  }

  override def shape: SinkShape[CameraFrame] = SinkShape(in)
}

@deprecated
class ShowImageGraph(cameraSource: CameraSource,
                     canvas: CanvasFrame,
                     killSwitch: SharedKillSwitch) extends GraphFactory[Sink[CameraSource, NotUsed]] with LazyLogging {

  override def createGraph: Sink[CameraSource, NotUsed] =
    Sink.fromGraph(GraphDSL.create(cameraSource) {
      implicit builder =>
        (source) =>
          import GraphDSL.Implicits._

          val converter = new OpenCVFrameConverter.ToIplImage()
          val showFrame = Flow[CameraFrame].via(killSwitch.flow)
            .map(f => (f, converter.convert(f.image))) // strange stuff, please simplify
            .map(f => {
            logger.info("processing image")
            canvas.showImage(f._2)
            f._1
          })

          val ShowNormalImage: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrame)
          val Ignore: Inlet[Any] = builder.add(Sink.ignore).in

          source.out ~> ShowNormalImage ~> Ignore

          SinkShape(Ignore)
    })

}
