package sentinel.camera.camera.stage

import akka.actor.ActorSystem
import akka.stream.scaladsl.Keep
import akka.stream.stage.GraphStage
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSource
import akka.stream.{ActorMaterializer, SinkShape}
import akka.testkit.TestKit
import org.bytedeco.javacpp.opencv_core.IplImage
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.bytedeco.javacv.{CanvasFrame, Frame}
import org.mockito.Matchers.{eq => mockEq}
import org.mockito.Mockito._
import sentinel.camera.camera.CameraFrame
import testutils.TestSystem.TestActorSystem
import testutils.{ShapeSpec, StopSystemAfterAll}

class ShowImageStageSpec extends TestKit(ActorSystem(TestActorSystem))
  with ShapeSpec
  with StopSystemAfterAll {

  implicit val materializer = ActorMaterializer()

  private val canvas = mock[CanvasFrame]
  private val converter = mock[ToIplImage]
  private val cameraFrame = mock[CameraFrame]
  private val frame = mock[Frame]
  private val image = mock[IplImage]

  private val underTest: GraphStage[SinkShape[CameraFrame]] = new ShowImageStage(canvas, converter)

  before {
    when(cameraFrame.image).thenReturn(image)
    when(converter.convert(image)).thenReturn(frame)
  }

  after {
    verifyNoMoreInteractions(canvas, converter, cameraFrame, frame, image)
    reset(canvas, converter, cameraFrame, frame, image)
  }

  "A ShowImageShape" should {
    "call dependencies properly" in {
      val upstream: TestPublisher.Probe[CameraFrame] = createGraphWithSink

      upstream.sendNext(cameraFrame)

      verify(converter).convert(image)
      verify(canvas).showImage(frame)
    }

    "exception in converter should not propagate" in {
      reset(converter)
      when(converter.convert(image))
        .thenThrow(new RuntimeException("boom"))
        .thenReturn(frame)

      val upstream: TestPublisher.Probe[CameraFrame] = createGraphWithSink

      upstream.sendNext(cameraFrame)
      upstream.sendNext(cameraFrame)

      verify(converter, times(2)).convert(image)
      verify(canvas).showImage(frame)
    }
  }

  private def createGraphWithSink = {
    val (upstream, _) =
      TestSource.probe[CameraFrame]
        .toMat(underTest)(Keep.both)
        .run()
    upstream
  }
}
