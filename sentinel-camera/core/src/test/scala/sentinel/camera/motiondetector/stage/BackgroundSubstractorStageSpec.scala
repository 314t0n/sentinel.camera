package sentinel.camera.motiondetector.stage

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSource
import akka.testkit.TestKit
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, OneInstancePerTest, WordSpecLike}
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import sentinel.camera.camera.{CameraFrame, MotionDetectFrame}
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class BackgroundSubstractorStageSpec extends TestKit(ActorSystem(TestActorSystem))
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with BeforeAndAfter
  with MockitoSugar {

  implicit val materializer = ActorMaterializer()

  private val backgroundSubstractor = mock[BackgroundSubstractor]
  private val cameraFrame = mock[CameraFrame]
  private val backgroundSubstractedFrame = mock[MotionDetectFrame]

  private val underTest = new BackgroundSubstractorStage(backgroundSubstractor)

  after {
    verifyNoMoreInteractions(backgroundSubstractor)
  }

  "A BackgroundSubstractorStage" should {

    "call dependencies properly" in {
      when(backgroundSubstractor.substractBackground(cameraFrame)).thenReturn(backgroundSubstractedFrame)
      val upstream: TestPublisher.Probe[CameraFrame] = createFlow

      upstream.sendNext(cameraFrame)

      verify(backgroundSubstractor).substractBackground(cameraFrame)
    }
  }

  private def createFlow = {
    val (upstream, _) =
      TestSource.probe[CameraFrame]
        .via(underTest)
        .toMat(Sink.ignore)(Keep.both)
        .run()
    upstream
  }
}
