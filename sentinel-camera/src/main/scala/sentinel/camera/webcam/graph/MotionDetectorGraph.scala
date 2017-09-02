package sentinel.camera.webcam.graph

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import sentinel.camera.motiondetector.MotionDetectStage
import sentinel.camera.motiondetector.bgsubtractor.{BackgroundSubtractorMOG2Factory, GaussianMixtureBasedBackgroundSubstractor}
import sentinel.camera.webcam.CameraFrame
import sentinel.camera.webcam.graph.CameraReaderGraph.{CameraSource, FrameFlow}
import sentinel.graph.GraphFactory
@deprecated
class MotionDetectorGraph(cameraSource: CameraSource) extends GraphFactory[FrameFlow] {

  override def createGraph() =
    Flow.fromGraph(GraphDSL.create(cameraSource) {
      implicit builder =>
        (source) =>
          import GraphDSL.Implicits._

          val mog = BackgroundSubtractorMOG2Factory()
          val substractor = new GaussianMixtureBasedBackgroundSubstractor(mog, 0.01)
          val motionDetectStage = new MotionDetectStage(substractor)
          val converter = new OpenCVFrameConverter.ToIplImage()

          val MotionDetector: FlowShape[CameraFrame, CameraFrame] = builder.add(motionDetectStage)

          source.out ~> MotionDetector

          FlowShape.of(MotionDetector.in, MotionDetector.out)
    })

}