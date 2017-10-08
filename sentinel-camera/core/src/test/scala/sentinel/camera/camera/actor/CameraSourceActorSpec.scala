package sentinel.camera.camera.actor

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.KillSwitch
import akka.stream.scaladsl.RunnableGraph
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.when
import org.scalatest.OneInstancePerTest
import org.scalatest.WordSpecLike
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.graph.factory.{CameraReaderGraphFactory, SourceBroadCastFactory}
import sentinel.router.Messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class CameraSourceActorSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with WordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with MockitoSugar {

  private val killSwitch                                  = mock[KillSwitch]
  private val sourceDummy: CameraSource                   = null
  private val broadcastDummy: RunnableGraph[CameraSource] = null
  private val broadcastFactory                            = mock[SourceBroadCastFactory]
  private val sourceFactory                               = mock[CameraReaderGraphFactory]
  private val underTest = TestActorRef(
    Props(new CameraSourceActor(sourceFactory, broadcastFactory)))

  "CameraActorSpec" when {

    "happy path" should {
      "start" in {
        when(sourceFactory.create(killSwitch)).thenReturn(sourceDummy)
        when(broadcastFactory.create(sourceDummy))
          .thenReturn(broadcastDummy)

        underTest ! Start(killSwitch)

        expectMsg(SourceInit(CameraSourcePublisher(broadcastDummy)))
        verify(sourceFactory).create(killSwitch)
        verify(broadcastFactory).create(sourceDummy)
      }
    }

    "error handling" should {
      "sourceFactory throws exception when start message received" in {
        val cause = "cause"
        when(sourceFactory.create(killSwitch))
          .thenThrow(new RuntimeException(cause))

        underTest ! Start(killSwitch)

        expectMsg(Error(cause))
        verify(sourceFactory).create(killSwitch)
        verifyZeroInteractions(broadcastFactory)
      }

      "broadcastFactory throws exception when start message received" in {
        val cause = "cause"
        when(sourceFactory.create(killSwitch)).thenReturn(sourceDummy)
        when(broadcastFactory.create(sourceDummy))
          .thenThrow(new RuntimeException(cause))

        underTest ! Start(killSwitch)

        expectMsg(Error(cause))
        verify(sourceFactory).create(killSwitch)
        verify(broadcastFactory).create(sourceDummy)
        verifyZeroInteractions(sourceFactory)
      }
    }
  }
}
