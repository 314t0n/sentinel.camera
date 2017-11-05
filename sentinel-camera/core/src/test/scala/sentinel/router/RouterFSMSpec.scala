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
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.reader.BroadCastRunnableGraph
import sentinel.camera.utils.settings.Settings
import sentinel.plugin.Plugin
import sentinel.router.RouterFSM.Add
import sentinel.router.RouterFSM.Remove
import sentinel.router.messages.Messages._
import sentinel.router.messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.duration._

class RouterFSMSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with WordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with Eventually
    with MockitoSugar {

  private implicit val ec    = system.dispatcher
  private val routingLogic   = BroadcastRoutingLogic()
  private val routeeA        = TestProbe()
  private val routeeB        = TestProbe()
  private val routees        = Vector(routeeA, routeeB)
  private val severalRoutees = SeveralRoutees(routees.map(_.ref).map(ActorRefRoutee))

  private val killSwitch = mock[KillSwitch]
  private val broadcast  = mock[BroadCastRunnableGraph]
  private val settings   = mock[Settings]
  when(settings.getDuration(any[String], any[TimeUnit]))
    .thenReturn(50 milliseconds)

  private val underTest = TestFSMRef(new RouterFSM(settings))

  "RouterFSM" when {

    "addinging plugin" should {
      "add plugin when Idle" in {
        val plugin = mock[Plugin]

        underTest ! Add(plugin)

        underTest.stateData shouldEqual stoppedRouter(plugin)
      }

      "add plugin multiple times should not cause any error" in {
        val plugin = mock[Plugin]

        underTest ! Add(plugin)
        underTest ! Add(plugin)
        underTest ! Add(plugin)
        underTest ! Add(plugin)

        underTest.stateData shouldEqual stoppedRouter(plugin)
      }

      "add plugin when Active should start the plugin" in {
        val plugin = mock[Plugin]
        underTest ! PluginStart(killSwitch, broadcast)
        expectMsg(Ready(Ok))

        underTest ! Add(plugin)

        underTest.stateData shouldEqual PluginRouter(Seq(plugin), Some(killSwitch), Some(broadcast))
        verify(plugin).start(any[PluginStart])
      }

      "add plugin throws exception when Active should respond with error message" in {
        val plugin = mock[Plugin]
        underTest ! PluginStart(killSwitch, broadcast)
        expectMsg(Ready(Ok))
        val message = "exception"
        when(plugin.start(any[PluginStart])).thenThrow(new RuntimeException(message))

        underTest ! Add(plugin)

        expectMsg(Error(message))
        underTest.stateData shouldEqual PluginRouter(Seq.empty, Some(killSwitch), Some(broadcast))
      }
    }

    "removing plugin" should {
      "remove plugin when Idle" in {
        val plugin = mock[Plugin]

        underTest ! Add(plugin)
        underTest ! Remove(plugin)

        underTest.stateData shouldEqual stoppedRouter()
      }

      "remove not contained plugin" in {
        val plugin = mock[Plugin]

        underTest ! Remove(plugin)

        underTest.stateData shouldEqual stoppedRouter()
      }

      "not contained plugin should not throw exception" in {
        val plugin = mock[Plugin]

        underTest ! Remove(plugin)

        underTest.stateData shouldEqual stoppedRouter()
      }

      "remove plugin when Active should stop the plugin" in {
        val plugin = mock[Plugin]
        underTest ! PluginStart(killSwitch, broadcast)
        expectMsg(Ready(Ok))

        underTest ! Add(plugin)
        underTest ! Remove(plugin)

        underTest.stateData shouldEqual PluginRouter(Seq.empty, Some(killSwitch), Some(broadcast))
        verify(plugin).stop
      }

      "remove plugin throws exception when Active should respond with error message" in {
        val plugin = mock[Plugin]
        underTest ! PluginStart(killSwitch, broadcast)
        expectMsg(Ready(Ok))
        val message = "exception"
        when(plugin.stop()).thenThrow(new RuntimeException(message))

        underTest ! Add(plugin)
        underTest ! Remove(plugin)

        expectMsg(Error(message))
        underTest.stateData shouldEqual PluginRouter(Seq.empty, Some(killSwitch), Some(broadcast))
      }
    }

    "state" should {
      "be Idle by default" in {
        underTest.stateName shouldBe Idle
      }

      "switch from Idle to Active" in {
        val plugin = mock[Plugin]

        underTest ! Add(plugin)
        underTest ! PluginStart(killSwitch, broadcast)

        expectMsg(Ready(Ok))
        underTest.stateName shouldBe Active
        verifyZeroInteractions(killSwitch, broadcast)
        verify(plugin).start(PluginStart(killSwitch, broadcast))
      }

      "switch from Idle to Active when plugin throw exception" in {
        val plugin = mock[Plugin]
        val message = "exception"
        when(plugin.start(any[PluginStart])).thenThrow(new RuntimeException(message))

        underTest ! Add(plugin)
        underTest ! PluginStart(killSwitch, broadcast)

        expectMsg(Error(message))
        underTest.stateName shouldBe Idle
        underTest.stateData shouldEqual PluginRouter(Seq(plugin), None, None)
        verifyZeroInteractions(killSwitch, broadcast)
      }

      "switch Active to Idle" in {
        val plugin = mock[Plugin]

        underTest ! Add(plugin)
        underTest ! PluginStart(killSwitch, broadcast)
        expectMsg(Ready(Ok))
        underTest ! Stop

        expectMsg(Ready(Finished))
        underTest.stateName shouldBe Idle
        verifyZeroInteractions(killSwitch, broadcast)
        verify(plugin).start(PluginStart(killSwitch, broadcast))
        verify(plugin).stop
      }

      "switch Active to Idle when plugin throws exception" in {
        val plugin = mock[Plugin]
        val message = "exception"
        when(plugin.stop()).thenThrow(new RuntimeException(message))

        underTest ! Add(plugin)
        underTest ! PluginStart(killSwitch, broadcast)
        expectMsg(Ready(Ok))
        underTest ! Stop

        expectMsg(Error(message))
        underTest.stateName shouldBe Idle
        verifyZeroInteractions(killSwitch, broadcast)
        verify(plugin).start(PluginStart(killSwitch, broadcast))
      }
    }
  }

  private def stoppedRouter(plugin: Plugin*) = PluginRouter(Seq(plugin: _*), None, None)

}
