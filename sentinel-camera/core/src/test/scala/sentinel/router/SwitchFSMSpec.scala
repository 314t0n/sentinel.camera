package sentinel.router

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, verifyZeroInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, OneInstancePerTest, WordSpecLike}
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch
import sentinel.camera.utils.settings.Settings
import sentinel.router.messages.Messages._
import sentinel.router.messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.duration._

class SwitchFSMSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with WordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with MockitoSugar {

  private implicit val ec         = system.dispatcher
  private val settings            = mock[Settings]
  private val systemInitializer = TestProbe()
  when(settings.getDuration(any[String], any[TimeUnit]))
    .thenReturn(50 milliseconds)
  private val underTest  = TestFSMRef(new SwitchFSM(systemInitializer.ref, settings))
  private val killSwitch = mock[GlobalKillSwitch]
  private val child      = TestProbe()

  "Switch" when {

    "happy path" should {

      "switch from Idle to Active" in {
        val request  = Start(killSwitch)
        val response = Ready(Ok)

        underTest ! request

          expectMsg(Ready(Ok))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
        expectTerminated(child.ref)
      }

      "switch from Active to Idle" in {
        setActiveState

        underTest ! Stop

        expectMsg(Ready(Finished))
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
      }

    }

    "error handling" should {

      "not switch from Idle to Active when child timeouts" in {
        val request = Start(killSwitch)
        underTest.stateName shouldBe Idle

        underTest ! request

        underTest.stateName shouldBe Waiting
        expectMsgAnyClassOf(3 seconds, classOf[Error])
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
        expectTerminated(child.ref)
      }

      "not switch when unknown error from child" in {
        val request = Start(killSwitch)
        underTest.stateName shouldBe Idle

        underTest ! request

        underTest.stateName shouldBe Waiting
        expectMsgAnyClassOf(3 seconds, classOf[Error])
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
        expectTerminated(child.ref)
      }

      "not switch from Active to Active" in {
        setActiveState

        underTest ! Start(killSwitch)

        expectMsg(Error(AlreadyStarted))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }

      "not switch from Idle to Idle" in {
        underTest.stateName shouldBe Idle

        underTest ! Stop

        expectMsg(Error(Finished))
        underTest.stateName shouldBe Idle
      }
    }
  }

  private def setActiveState = {
    val request = Start(killSwitch)
    underTest ! request
    child.expectMsg(request)
    child.reply(Ready(Ok))
    expectMsg(Ready(Ok))
    underTest.stateName shouldBe Active
  }
}
