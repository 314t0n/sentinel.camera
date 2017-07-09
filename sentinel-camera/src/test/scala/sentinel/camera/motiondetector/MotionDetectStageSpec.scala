package sentinel.camera.motiondetector

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.testkit.TestKit
import org.mockito.Mockito.verifyNoMoreInteractions
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, OneInstancePerTest, WordSpecLike}
import sentinel.camera.motiondetector.bgsubtractor.BackgroundSubstractor
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class MotionDetectStageSpec extends TestKit(ActorSystem(TestActorSystem))
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with BeforeAndAfter
  with MockitoSugar {

  implicit val materializer = ActorMaterializer()

  private val backgroundSubstractor = mock[BackgroundSubstractor]
  private val killSwitch = KillSwitches.shared("switch")
  private val underTest = new MotionDetectStage(backgroundSubstractor)

  after {
    verifyNoMoreInteractions(backgroundSubstractor)
  }
}
