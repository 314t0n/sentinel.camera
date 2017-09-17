package sentinel.router

import akka.actor.ActorSystem
import akka.routing.ActorRefRoutee
import akka.routing.BroadcastRoutingLogic
import akka.routing.Routee
import akka.routing.SeveralRoutees
import akka.stream.KillSwitch
import akka.testkit.ImplicitSender
import akka.testkit.TestFSMRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.scalatest.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.WordSpecLike
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.graph.SourceBroadCast
import sentinel.router.Messages._
import testutils.StopSystemAfterAll
import scala.concurrent.duration._
import testutils.TestSystem.TestActorSystem

class PluginRouterSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with WordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with MockitoSugar {

  private implicit val ec  = system.dispatcher
  private val routingLogic = BroadcastRoutingLogic()
  private val cameraSource = TestProbe()
  private val routeeA      = TestProbe()
  private val routeeB      = TestProbe()
  private val routees      = Vector(routeeA, routeeB)
  private val underTest = TestFSMRef(
    new PluginRouter(cameraSource.ref,
                     routingLogic,
                     SeveralRoutees(routees.map(_.ref).map(ActorRefRoutee(_)))))
  private val killSwitch = mock[KillSwitch]
  private val broadcast  = mock[SourceBroadCast]

  "PluginRouter" when {

    "happy path" should {

      "switch from Idle to Active" in {
        val request  = Start(killSwitch)
        val response = Ready(Ok)
        underTest.stateName shouldBe Idle

        underTest ! request

        cameraSource.expectMsg(request)
        cameraSource.reply(SourceInit(broadcast))
        routees foreach { r =>
          r.expectMsg(PluginStart(killSwitch, broadcast))
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
