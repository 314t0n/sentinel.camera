package sentinel.camera.camera.actor

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.scaladsl.RunnableGraph
import akka.stream.ActorMaterializer
import akka.stream.KillSwitch
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.scalatest.WordSpecLike
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.reader.BroadCastRunnableGraph
import sentinel.camera.camera.reader.BroadcastMateralizer
import sentinel.router.messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.Promise

class CameraSourceActorSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with WordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with MockitoSugar {

  implicit val materializer                      = ActorMaterializer()
  private val killSwitch                         = mock[KillSwitch]
  private val graph: RunnableGraph[CameraSource] = null
  private val broadcastDummy                     = BroadCastRunnableGraph(graph)
  private val broadCastMateralizer               = mock[BroadcastMateralizer]
  private val underTest =
    TestActorRef(Props(new CameraSourceActor(broadCastMateralizer)(materializer)))

  private val promise = Promise[BroadCastRunnableGraph]()

  "CameraActorSpec" when {

    /*"happy path" should {
      "start" in {
        when(broadCastMateralizer.create(killSwitch)).thenReturn(promise)

        underTest ! Start(killSwitch)
        promise success broadcastDummy

        expectMsg(SourceInit(broadcastDummy))
        verify(broadCastMateralizer).create(killSwitch)
      }
    }

    "error handling" should {
      "sourceFactory throws exception when start message received" in {
        val cause = "cause"
        when(broadCastMateralizer.create(killSwitch)).thenReturn(promise)

        underTest ! Start(killSwitch)
        promise failure new Exception(cause)

        expectMsg(Error(cause))
        verify(broadCastMateralizer).create(killSwitch)
        verifyZeroInteractions(killSwitch)
      }

      "broadcastFactory throws exception when start message received" in {
        val cause = "cause"
        when(broadCastMateralizer.create(killSwitch))
          .thenThrow(new RuntimeException(cause))

        underTest ! Start(killSwitch)

        expectMsg(Error(cause))
        verify(broadCastMateralizer).create(killSwitch)
        verifyZeroInteractions(killSwitch)
      }
    }*/
  }
}
