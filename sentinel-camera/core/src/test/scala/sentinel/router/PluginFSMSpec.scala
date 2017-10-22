package sentinel.router

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.routing.ActorRefRoutee
import akka.routing.BroadcastRoutingLogic
import akka.routing.SeveralRoutees
import akka.stream.KillSwitch
import akka.testkit.ImplicitSender
import akka.testkit.TestFSMRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import org.mockito.Matchers.any
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.reader.BroadCastRunnableGraph
import sentinel.camera.utils.settings.Settings
import sentinel.router.Messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.duration._

class PluginFSMSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with WordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with Eventually
    with MockitoSugar {

  private implicit val ec  = system.dispatcher
  private val routingLogic = BroadcastRoutingLogic()
  private val routeeA      = TestProbe()
  private val routeeB      = TestProbe()
  private val routees      = Vector(routeeA, routeeB)
  private val severalRoutees = SeveralRoutees(
    routees.map(_.ref).map(ActorRefRoutee))

  private val killSwitch = mock[KillSwitch]
  private val broadcast  = mock[BroadCastRunnableGraph]
  private val settings   = mock[Settings]
  when(settings.getDuration(any[String], any[TimeUnit]))
    .thenReturn(50 milliseconds)

  private val underTest = TestFSMRef(
    new PluginFSM(routingLogic, severalRoutees, settings))

  "PluginFSM " when {

    "happy path" should {

      "be Idle by defautl" in {
        underTest.stateName shouldBe Idle
      }

      "switch from Idle to Active" in {
        underTest ! PluginStart(killSwitch, broadcast)

        routees foreach { routee =>
          routee.expectMsg(PluginStart(killSwitch, broadcast))
          routee.reply(Ready(Ok))
        }
        expectMsg(Ready(Ok))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }

      "switch from Idle to Active when no routees added" in {
        val underTest = TestFSMRef(
          new PluginFSM(routingLogic,
                           SeveralRoutees(Vector.empty),
                           settings))

        underTest ! PluginStart(killSwitch, broadcast)

        expectMsg(Ready(Ok))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch)
      }

      "switch from Active to Idle" in {
        setActiveState()

        underTest ! Stop

        routees foreach { r =>
          r.expectMsg(Stop)
          r.reply(Ready(Finished))
        }
        expectMsg(Ready(Finished))
        underTest.stateName shouldBe Idle
        verifyZeroInteractions(killSwitch)
      }

      "switch from Active to Idle when no routees added" in {
        val underTest = TestFSMRef(
          new PluginFSM(routingLogic,
                           SeveralRoutees(Vector.empty),
                           settings))
        underTest ! PluginStart(killSwitch, broadcast)
        expectMsg(Ready(Ok))

        underTest ! Stop

        expectMsg(Ready(Finished))
        underTest.stateName shouldBe Idle
        verifyZeroInteractions(killSwitch)
      }
    }

    "error handling" when {

      "routees timeout handled" when {
        "switch from Idle to Active" in {
          underTest ! PluginStart(killSwitch, broadcast)

          routees foreach { routee =>
            routee.expectMsg(PluginStart(killSwitch, broadcast))
          }
          expectMsgAnyClassOf(4 seconds, classOf[Error])
          eventually {
            underTest.stateName shouldBe Idle
          }
          verifyZeroInteractions(killSwitch)
        }

        "switch from Active to Idle" in {
          setActiveState()

          underTest ! Stop

          routees foreach { routee =>
            routee.expectMsg(Stop)
          }
          expectMsgAnyClassOf(4 seconds, classOf[Error])
          eventually {
            underTest.stateName shouldBe Idle
          }
          verifyZeroInteractions(killSwitch)
        }
      }

      "not switch from Active to Active" in {
        setActiveState()

        underTest ! PluginStart(killSwitch, broadcast)

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

  private def setActiveState() = {
    underTest ! PluginStart(killSwitch, broadcast)
    routees foreach { routee =>
      routee.expectMsg(PluginStart(killSwitch, broadcast))
      routee.reply(Ready(Ok))
    }
    expectMsg(Ready(Ok))
  }
}
