package sentinel.camera.camera.reader

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.KillSwitch
import akka.stream.SharedKillSwitch
import akka.stream.scaladsl.RunnableGraph
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import org.mockito.Mockito.when
import org.scalatest.AsyncWordSpecLike
import org.scalatest.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.WordSpecLike
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.Promise

class CameraReaderFactorySpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with AsyncWordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with MockitoSugar {

  implicit val materializer        = ActorMaterializer()
  private val killSwitch           = None.orNull
  private val graph                = None.orNull
  private val broadcastDummy       = BroadCastRunnableGraph(graph)
  private val broadCastMateralizer = mock[BroadcastMaterializer]
  private val promise              = Promise[BroadCastRunnableGraph]()

//  private val underTest = new CameraReaderFactory(broadCastMateralizer)
//
//  "CameraReaderFactory" should {
//
//    "call broadCastMateralizer and return broadcast" in {
//      val gks = KillSwitches.GlobalKillSwitch(killSwitch)
//      when(broadCastMateralizer.create(gks)).thenReturn(Promise.successful(broadcastDummy))
//
//      val result = underTest.create(gks)
//
//      result.future.map(expectedBroadCast => {
//        expectedBroadCast shouldEqual broadcastDummy
//      })
//    }

//  }

}
