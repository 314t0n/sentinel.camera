package sentinel.camera.webcam.graph

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.{FlowShape, Outlet, SharedKillSwitch, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Source}
import com.typesafe.scalalogging.LazyLogging
import org.bytedeco.javacv.{Frame, OpenCVFrameConverter}
import sentinel.camera.webcam.CameraFrame
import sentinel.camera.webcam.graph.CameraReaderGraph.CameraSource
import sentinel.graph.GraphFactory

object CameraReaderGraph {
  type CameraSource = Source[CameraFrame, NotUsed] //framesoource imagesource
  type FrameFlow = Flow[CameraFrame, CameraFrame, NotUsed]
}
// rename to CameraReaderSource?
class CameraReaderGraph(webcamSource: Source[Frame, NotUsed],
                        tickingSource: Source[Int, Cancellable],
                        killSwitch: SharedKillSwitch) extends GraphFactory[CameraSource] with LazyLogging {

  override def createGraph(): CameraSource =
    Source.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._
        // why u no depenendency?
        val converter = new OpenCVFrameConverter.ToIplImage()

        val IplImageConverter: FlowShape[Frame, CameraFrame] = builder.add(Flow[Frame]
          .via(killSwitch.flow)
          .map(converter.convert)
          .map(CameraFrame(_)))

        val WebCam: Outlet[Frame] = builder.add(webcamSource).out

        val webcamStream = WebCam
          .via(killSwitch.flow)
          .zip(tickingSource)
          .map(_._1)

        val stream = webcamStream ~> IplImageConverter

        SourceShape(stream.outlet)
    })
}
