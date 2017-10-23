package sentinel.router

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.KillSwitch
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, verifyZeroInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, OneInstancePerTest, WordSpecLike}
import sentinel.camera.utils.settings.Settings
import sentinel.router.messages.Messages._
import sentinel.router.messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.duration._

class SwitchFSMSpec extends TestKit(ActorSystem(TestActorSystem))
  with ImplicitSender
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with Matchers
  with MockitoSugar {

  private implicit val ec = system.dispatcher
  private val router = TestProbe()
  private val settings = mock[Settings]
  when(settings.getDuration(any[String], any[TimeUnit]))
    .thenReturn(50 milliseconds)
  private val underTest = TestFSMRef(new SwitchFSM(router.ref, settings))
  private val killSwitch = mock[KillSwitch]

  "Switch" when {

    "happy path" should {

      "switch from Idle to Active" in {
        val request = Start(killSwitch)
        val response = Ready(Ok)
        underTest.stateName shouldBe Idle

        underTest ! request

        router.expectMsg(request)
        router.reply(response)
        expectMsg(response)
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }

      "switch from Active to Idle" in {
        setActiveState

        underTest ! Stop

        router.expectMsg(Stop)
        router.reply(Ready(Finished))
        expectMsg(Ready(Finished))
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
      }

    }

    "error handling" should {

      "not switch from Idle to Active when Router timeouts" in {
        val request = Start(killSwitch)
        underTest.stateName shouldBe Idle

        underTest ! request

        underTest.stateName shouldBe Waiting
        router.expectMsg(request)
        expectMsgAnyClassOf(3 seconds, classOf[Error])
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
      }

      "not switch from Active to Idle when Router timeouts" in {
        setActiveState

        underTest ! Stop

        underTest.stateName shouldBe Waiting
        router.expectMsg(Stop)
        expectMsgAnyClassOf(3 seconds, classOf[Error])
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
      }

      "not switch unknown error from Router" in {
        val request = Start(killSwitch)
        underTest.stateName shouldBe Idle

        underTest ! request

        underTest.stateName shouldBe Waiting
        router.expectMsg(request)
        router.reply("Unknown")
        expectMsgAnyClassOf(3 seconds, classOf[Error])
        underTest.stateName shouldBe Idle
        verify(killSwitch).shutdown()
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
    router.expectMsg(request)
    router.reply(Ready(Ok))
    expectMsg(Ready(Ok))
    underTest.stateName shouldBe Active
  }
}
