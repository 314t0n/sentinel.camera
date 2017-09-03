package sentinel.camera.motiondetector.bgsubstractor

import org.bytedeco.javacpp.opencv_core.{IplImage, Mat}
import org.bytedeco.javacpp.opencv_video.BackgroundSubtractorMOG2
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfter, Matchers, OneInstancePerTest, WordSpecLike}
import sentinel.camera.motiondetector.bgsubtractor.GaussianMixtureBasedBackgroundSubstractor
import sentinel.camera.camera.CameraFrame

class GaussianMixtureBasedBackgroundSubstractorSpec extends WordSpecLike
  with BeforeAndAfter
  with MockitoSugar
  with Matchers {

  private val backgroundSubtractorMOG2 = mock[BackgroundSubtractorMOG2]
  private val learningRate = 1.0
  private val underTest = new GaussianMixtureBasedBackgroundSubstractor(backgroundSubtractorMOG2, learningRate)

  "A GaussianMixtureBasedBackgroundSubstractor" can {

    "substractBackground" should {

      "call backgroundSubtractorMOG2 apply" in {
        val source = new IplImage(new Mat(1, 1, 1))

        underTest.substractBackground(CameraFrame(source))

        verify(backgroundSubtractorMOG2).apply(any[Mat], any[Mat], mockEq(learningRate))
      }
    }

    "close" should {

      "backgroundSubtractorMOG2" in {
        underTest.close()

        verify(backgroundSubtractorMOG2).close()
      }
    }
  }
}
