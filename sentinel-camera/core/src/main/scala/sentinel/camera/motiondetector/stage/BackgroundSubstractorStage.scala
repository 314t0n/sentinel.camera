package sentinel.camera.motiondetector.stage

import akka.stream._
import akka.stream.stage._
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import sentinel.camera.camera.CameraFrame

class BackgroundSubstractorStage(backgroundSubstractor: BackgroundSubstractor)
    extends GraphStage[FlowShape[CameraFrame, CameraFrame]]
    with LazyLogging {

  private val in  = Inlet[CameraFrame]("MotionDetect.in")
  private val out = Outlet[CameraFrame]("MotionDetect.out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private def substractBackground(frame: CameraFrame) = backgroundSubstractor.substractBackground(frame)

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          push(out, substractBackground(grab(in)))
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }

  override def shape: FlowShape[CameraFrame, CameraFrame] = FlowShape.of(in, out)
}
