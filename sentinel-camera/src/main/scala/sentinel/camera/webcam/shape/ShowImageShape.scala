package sentinel.camera.webcam.shape

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import sentinel.camera.webcam.CameraFrame

import scala.util.Try

/**
  * Sink to display Frames on canvas
  *
  * @param canvas a JFrame that displays the given frame
  * @param converter converts frame to CanvasFrame
  */
class ShowImageShape(canvas: CanvasFrame, converter: ToIplImage) extends GraphStage[SinkShape[CameraFrame]] {

  private val in = Inlet[CameraFrame]("ShowImage.in")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        Try {
          val elem: CameraFrame = grab(in)
          canvas.showImage(converter.convert(elem.image))
        }
        pull(in)
      }
    })

    override def preStart(): Unit = pull(in)
  }

  override def shape: SinkShape[CameraFrame] = SinkShape(in)
}