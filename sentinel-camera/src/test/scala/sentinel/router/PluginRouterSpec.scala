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

      "be Idle by defautl" in {
        underTest.stateName shouldBe Idle
      }

      "switch from Idle to Active" in {
        underTest ! Start(killSwitch)

        cameraSource.expectMsg(Start(killSwitch))
        cameraSource.reply(SourceInit(broadcast))
        routees foreach { routee =>
          routee.expectMsg(PluginStart(killSwitch, broadcast))
          routee.reply(Ready(Ok))
        }
        expectMsg(Ready(Ok))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }

      "switch from Active to Idle" in {
        setActiveState

        underTest ! Stop

        cameraSource.expectMsg(Stop)
        cameraSource.reply(Ready(Finished))
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
    underTest ! Start(killSwitch)
    cameraSource.expectMsg(Start(killSwitch))
    cameraSource.reply(SourceInit(broadcast))
    routees foreach { routee =>
      routee.expectMsg(PluginStart(killSwitch, broadcast))
      routee.reply(Ready(Ok))
    }
    expectMsg(Ready(Ok))
  }
}
