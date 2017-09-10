package sentinel.router

import akka.actor.ActorSystem
import akka.stream.KillSwitch
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, OneInstancePerTest, WordSpecLike}
import sentinel.router.Messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class BuncherSpec extends TestKit(ActorSystem(TestActorSystem))
  with ImplicitSender
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with Matchers
  with MockitoSugar {

  private val underTest = TestFSMRef(new Buncher)
  private val killSwitch = mock[KillSwitch]

  "Buncher" when {

    "start message" should {
      "from Idle to Active response ok" in {
        underTest.stateName shouldBe Idle

        underTest ! Start(killSwitch)

        expectMsg(Ready(Ok))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }

      "from Active to Active response error" in {
        underTest ! Start(killSwitch)
        underTest.stateName shouldBe Active

        underTest ! Start(killSwitch)

        expectMsg(Ready(Ok))
        expectMsg(Error(AlreadyStarted))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }
    }

    "stop message" should {

      "from Idle to Idle response error" in {
        underTest.stateName shouldBe Idle

        underTest ! Stop

        expectMsg(Error(Finished))
        underTest.stateName shouldBe Idle
      }

      "from Active to Idle response ok" in {
        underTest ! Start(killSwitch)
        underTest.stateName shouldBe Active

        underTest ! Stop

        expectMsg(Ready(Ok))
        expectMsg(Ready(Finished))
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
      }
    }
  }
}
