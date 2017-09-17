package sentinel.router

import akka.actor.ActorSystem
import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Routee, SeveralRoutees}
import akka.stream.KillSwitch
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.scalatest.{Matchers, OneInstancePerTest, WordSpecLike}
import org.scalatest.mockito.MockitoSugar
import sentinel.router.Messages.{AlreadyStarted, Error, Finished, Ok, Ready, Start, Stop}
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class PluginRouterSpec extends TestKit(ActorSystem(TestActorSystem))
  with ImplicitSender
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with Matchers
  with MockitoSugar {

  private implicit val ec = system.dispatcher
  private val routingLogic = BroadcastRoutingLogic()
  private val routeeA = TestProbe()
  private val routeeB = TestProbe()
  private val routees = Vector(routeeA, routeeB)
  private val underTest = TestFSMRef(
    new PluginRouter(routingLogic,
      SeveralRoutees(routees.map(_.ref).map(ActorRefRoutee(_)))))
  private val killSwitch = mock[KillSwitch]

  "Switch" when {

    "happy path" should {

      "switch from Idle to Active" in {
        val request = Start(killSwitch)
        val response = Ready(Ok)
        underTest.stateName shouldBe Idle

        underTest ! request

        routees foreach { r =>
          r.expectMsg(request)
          r.reply(response)
        }
        expectMsg(response)
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }

      "switch from Active to Idle" in {
        setActiveState

        underTest ! Stop

        routees foreach { r =>
          r.expectMsg(Stop)
          r.reply(Ready(Finished))
        }
        expectMsg(Ready(Finished))
        underTest.stateName shouldBe Idle
      }

    }

    "error handling" should {

      // TODO rotuee timeouts

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
    routees foreach { r =>
      r.expectMsg(request)
      r.reply(Ready(Ok))
    }
    expectMsg(Ready(Ok))
    underTest.stateName shouldBe Active
  }
}
