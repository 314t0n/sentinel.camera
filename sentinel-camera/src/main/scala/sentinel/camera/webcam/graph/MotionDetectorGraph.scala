package sentinel.camera.webcam.graph

import akka.NotUsed
import akka.stream.{ClosedShape, FlowShape, Inlet, SharedKillSwitch}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink}
import org.bytedeco.javacv.{CanvasFrame, OpenCVFrameConverter}
import sentinel.camera.motiondetector.MotionDetectStage
import sentinel.camera.motiondetector.bgsubtractor.{BackgroundSubtractorMOG2Factory, GaussianMixtureBasedBackgroundSubstractor}
import sentinel.camera.webcam.CameraFrame
import sentinel.camera.webcam.graph.CameraReaderGraph.CameraSource
import sentinel.graph.GraphFactory

class MotionDetectorGraph(cameraSource: CameraSource,
                          canvas: CanvasFrame,
                          killSwitch: SharedKillSwitch) extends GraphFactory[RunnableGraph[NotUsed]] {

  override def createGraph: RunnableGraph[NotUsed] = ???

  //    RunnableGraph.fromGraph(GraphDSL.create(cameraSource) {
  //      implicit builder =>
  //        (source) =>
  //          import GraphDSL.Implicits._
  //
  //          val mog = BackgroundSubtractorMOG2Factory()
  //          val substractor = new GaussianMixtureBasedBackgroundSubstractor(mog, 0.01)
  //          val motionDetectStage = new MotionDetectStage(substractor)
  //          val converter = new OpenCVFrameConverter.ToIplImage()
  //
  //
  //          val ShowNormalImage: FlowShape[CameraFrame, CameraFrame] = builder.add(showFrame)
  //          val Ignore: Inlet[Any] = builder.add(Sink.ignore).in
  //
  //          source.out ~>  ~> MotionDetector ~> Ignore
  //
  //          ClosedShape
  //    })
}