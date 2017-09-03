package sentinel.camera.camera

import akka.actor.{ActorSystem, DeadLetterSuppression}
import akka.contrib.throttle.Throttler.SetTarget
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.bytedeco.javacv.FrameGrabber
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Ignore, OneInstancePerTest, WordSpecLike}
import sentinel.camera.camera.WebcamFramePublisherSpec._
import sentinel.camera.camera.actor.WebcamFramePublisher
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

object WebcamFramePublisherSpec{
  private def OneRequest = Request(1)
}
@Ignore
class WebcamFramePublisherSpec extends TestKit(ActorSystem(TestActorSystem))
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with MockitoSugar {

  private val grabber = mock[FrameGrabber]
  private val throttler = TestProbe()
  private val underTest = TestActorRef(new WebcamFramePublisher(grabber, throttler.ref))

  "A WebcamFramePublisher" can {

    "receive Cancel message" should {

      "stop calling grabber" in {
        // Given
        underTest ! OneRequest
        // When
        underTest ! Cancel
        underTest ! OneRequest
        // Then
        verify(grabber).grab()
        verify(grabber).close()
        verifyNoMoreInteractions(grabber)
      }

      "stop calling throttler" in {
        // Given
        underTest ! OneRequest
        // When
        underTest ! Cancel
        // Then
        throttler.expectMsg(SetTarget(Some(underTest.underlyingActor.self)))
        throttler.expectMsgAnyClassOf(classOf[DeadLetterSuppression]) // Continue
        throttler.expectNoMsg()
      }
    }

    "has a TimerBasedThrottler dependency that" should {
      "call SetTarget when constructor is called" in {
        // Then
        throttler.expectMsg(SetTarget(Some(underTest.underlyingActor.self)))
      }

      "called with correct message when Request message sent to the actor" in {
        // When
        underTest ! OneRequest
        // Then
        throttler.expectMsg(SetTarget(Some(underTest.underlyingActor.self)))
        throttler.expectMsgAnyClassOf(classOf[DeadLetterSuppression]) // Continue
      }
    }

    "has a FrameGrabber dependency that" should {
      "call grab when Request message sent to the actor" in {
        // When
        underTest ! OneRequest
        // Then
        verify(grabber).grab()
      }
      "call close when Cancel message sent to the actor" in {
        // When
        underTest ! Cancel
        // Then
        verify(grabber).close()
      }
    }
  }
}
