package sentinel.camera.webcam.graph

import akka.NotUsed
import akka.stream.{ClosedShape, FlowShape, Inlet, SharedKillSwitch}
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, MergeHub, RunnableGraph, Sink}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import sentinel.camera.webcam.CameraFrame
import sentinel.camera.webcam.graph.CameraReaderGraph.CameraSource
import sentinel.graph.GraphFactory

class ShowImageGraph(cameraSource: CameraSource,
                     canvas: CanvasFrame,
                     killSwitch: SharedKillSwitch) extends GraphFactory[RunnableGraph[NotUsed]] with LazyLogging{

  override def createGraph: RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create(cameraSource) {
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

          ClosedShape
    })

}
