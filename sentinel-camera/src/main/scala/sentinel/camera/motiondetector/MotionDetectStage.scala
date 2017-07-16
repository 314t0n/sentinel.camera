package sentinel.camera.motiondetector

import akka.stream._
import akka.stream.stage._
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import sentinel.camera.webcam.CameraFrame

class MotionDetectStage(backgroundSubstractor: BackgroundSubstractor)
  extends GraphStage[FlowShape[CameraFrame, CameraFrame]] with LazyLogging {

  val in = Inlet[CameraFrame]("MotionDetect.in")
  val out = Outlet[CameraFrame]("MotionDetect.out")
  val motionDetectShape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private def motionDetect(frame: CameraFrame) = {
        backgroundSubstractor.substractBackground(frame)
      }

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          push(out, motionDetect(grab(in)))
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })

    }

  override def shape: FlowShape[CameraFrame, CameraFrame] = motionDetectShape
}
