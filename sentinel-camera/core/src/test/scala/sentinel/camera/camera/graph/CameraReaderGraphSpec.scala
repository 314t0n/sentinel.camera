package sentinel.camera.camera.graph

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.{ActorMaterializer, SharedKillSwitch}
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.bytedeco.javacv.{Frame, FrameGrabber}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, OneInstancePerTest, WordSpecLike}
import sentinel.camera.camera.stage.CameraReaderStage
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class CameraReaderGraphSpec extends TestKit(ActorSystem(TestActorSystem))
with WordSpecLike
with OneInstancePerTest
with StopSystemAfterAll
with BeforeAndAfter
with MockitoSugar {

  implicit val materializer = ActorMaterializer()
//
//  private val webcamSource = mock[Source[Frame, NotUsed]]
//  private val tickingSource = mock[Source[Int, Cancellable]]
//  private val killSwitch = mock[SharedKillSwitch]
//  private val underTest = new CameraReaderGraph(webcamSource, tickingSource, killSwitch)
//
//  "xxxxxxxxxxx" should {
//
//    "xxxxxxxxxxxxxxxxxxxxx" in {
//
//      underTest.createGraph()
//
//
//    }
//
//  }



}
