package sentinel.camera.webcam

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.testkit.TestKit
import org.bytedeco.javacv.{Frame, FrameGrabber}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, OneInstancePerTest, WordSpecLike}
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class WebcamStageSpec extends TestKit(ActorSystem(TestActorSystem))
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with BeforeAndAfter
  with MockitoSugar {

  implicit val materializer = ActorMaterializer()

  private val grabber = mock[FrameGrabber]
  private val killSwitch = KillSwitches.shared("switch")
  private val underTest = new WebcamStage(grabber)

  after {
    verifyNoMoreInteractions(grabber)
  }

  "A WebcamStage" should {

    "not push out any message when grabber returns null" in {
      when(grabber.grab()).thenReturn(null)

      val sink = createSource(underTest)
      sink.request(1).expectNoMsg()

      killSwitch.shutdown()

      sink.expectComplete()
      verify(grabber).grab()
      verify(grabber).close()
    }

    "push out frame correctly" in {
      val fake = new Frame()
      when(grabber.grab()).thenReturn(fake)

      val sink = createSource(underTest)
      sink.request(1)

      killSwitch.shutdown()

      sink.expectNext(fake).expectComplete()
      verify(grabber).grab()
      verify(grabber).close()
    }

    "close the grabber when immedietely shutdown" in {
      val sink = createSource(underTest)

      killSwitch.shutdown()

      sink.expectSubscriptionAndComplete()
      verify(grabber).close()
    }
  }

  private def createSource(webcamStage: WebcamStage) = {
    val (_, sink) = Source.fromGraph(webcamStage)
      .via(killSwitch.flow)
      .toMat(TestSink.probe)(Keep.both)
      .run()
    sink
  }
}
