package sentinel.camera.motiondetect

import akka.stream._
import akka.stream.stage._
import sentinel.camera.motiondetect.bgsubtractor.GaussianMixtureBasedBackgroundSubstractor
import sentinel.camera.webcam.CameraFrame

class MotionDetectStage extends GraphStage[FlowShape[CameraFrame, CameraFrame]] {

  val in = Inlet[CameraFrame]("MotionDetect.in")
  val out = Outlet[CameraFrame]("MotionDetect.out")
  val motionDetectShape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      val substractor = new GaussianMixtureBasedBackgroundSubstractor()

      private def motionDetect(frame: CameraFrame) = {
        substractor.substractBackground(frame)
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
