package sentinel.camera.camera.reader

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.KillSwitch
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory
import sentinel.camera.camera.reader.BroadcastMateralizer.StreamClosedError
import sentinel.camera.utils.settings.Settings
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.Future
import scala.concurrent.TimeoutException

import BroadcastMateralizerSpec._

object BroadcastMateralizerSpec{
  private val exception = new RuntimeException("message")
}

class BroadcastMateralizerSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with AsyncWordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with MockitoSugar {

  private implicit val materializer     = ActorMaterializer()
  private val killSwitch                = mock[KillSwitch]
  private val sourceDummy: CameraSource = null
  private val broadcastDummy: BroadCastRunnableGraph =
    mock[BroadCastRunnableGraph]
  private val broadcastFactory    = mock[SourceBroadCastFactory]
  private val cameraReaderFactory = mock[CameraReaderGraphFactory]
  private val settings            = mock[Settings]
  private val underTest =
    new BroadcastMateralizer(cameraReaderFactory, broadcastFactory, settings)(
      materializer)

  "BroadcastMateralizer" when {

    "create" should {
      "return broadcast when Future timing out" in {
        when(cameraReaderFactory.create(killSwitch)).thenReturn(sourceDummy)
        when(broadcastFactory.create(sourceDummy))
          .thenReturn(broadcastDummy)
        when(broadcastDummy.toFuture())
          .thenThrow(new TimeoutException)

        val result = underTest.create(killSwitch)

        result.future map { event =>
          verify(cameraReaderFactory).create(killSwitch)
          verify(broadcastFactory).create(sourceDummy)
          event shouldBe broadcastDummy
        }
      }

      "return exception when exception occurs in Future" in {
        when(cameraReaderFactory.create(killSwitch)).thenReturn(sourceDummy)
        when(broadcastFactory.create(sourceDummy))
          .thenReturn(broadcastDummy)
        when(broadcastDummy.toFuture())
          .thenReturn(Future.failed(exception))

        val result = underTest.create(killSwitch)

        result.future.failed map { event =>
          verify(cameraReaderFactory).create(killSwitch)
          verify(broadcastFactory).create(sourceDummy)
          event shouldBe exception
        }
      }

      "return exception when Future throws exception" in {
        when(cameraReaderFactory.create(killSwitch)).thenReturn(sourceDummy)
        when(broadcastFactory.create(sourceDummy))
          .thenReturn(broadcastDummy)
        when(broadcastDummy.toFuture())
          .thenThrow(exception)

        val result = underTest.create(killSwitch)

        result.future.failed map { event =>
          verify(cameraReaderFactory).create(killSwitch)
          verify(broadcastFactory).create(sourceDummy)
          event shouldBe exception
        }
      }

      "return exception when stream stops before timeout" in {
        when(cameraReaderFactory.create(killSwitch)).thenReturn(sourceDummy)
        when(broadcastFactory.create(sourceDummy))
          .thenReturn(broadcastDummy)
        when(broadcastDummy.toFuture())
          .thenReturn(Future.successful(Done))

        val result = underTest.create(killSwitch)

        result.future.failed map { event =>
          verify(cameraReaderFactory).create(killSwitch)
          verify(broadcastFactory).create(sourceDummy)
          event.getMessage shouldBe StreamClosedError
        }
      }
    }
  }
}
