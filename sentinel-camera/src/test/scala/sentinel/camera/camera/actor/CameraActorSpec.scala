package sentinel.camera.camera.actor

import akka.actor.{ActorSystem, Props}
import akka.stream.{KillSwitch, SharedKillSwitch}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{OneInstancePerTest, WordSpecLike}
import sentinel.camera.camera.actor.CameraActor.{Start, Stop}
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class CameraActorSpec extends TestKit(ActorSystem(TestActorSystem))
  with ImplicitSender
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with MockitoSugar {

  private val killSwitch = mock[KillSwitch]
  private val underTest = TestActorRef(Props(new CameraActor()))

  "CameraActorSpec" ignore {

    "start message" should {
      "response ok message" in {
        underTest ! Start(killSwitch)
        expectMsg("ok")
      }

      "response error message" in {
        underTest ! Start(killSwitch)
        underTest ! Start(killSwitch)
        expectMsg("ok")
      }
    }

    "stop message" should {
      "response ok message" in {
        underTest ! Stop
        expectMsg("ok")
        Mockito.verifyZeroInteractions(killSwitch)
      }
    }

    "start and stop message" should {
      "response ok when called in right order" in {
        underTest ! Start(killSwitch)
        expectMsg("ok")
        underTest ! Stop
        expectMsg("ok")
        Mockito.verify(killSwitch).shutdown()
      }
    }
  }
}
