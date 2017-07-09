package sentinel.camera.storage

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.bytedeco.javacpp.opencv_core.IplImage
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{OneInstancePerTest, WordSpecLike}
import sentinel.camera.storage.Storage.Save
import sentinel.camera.utils.CVUtils
import sentinel.camera.webcam.CameraFrame
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

class FileStorageSpec extends TestKit(ActorSystem(TestActorSystem))
  with WordSpecLike
  with OneInstancePerTest
  with StopSystemAfterAll
  with MockitoSugar {

  private val filePath = ""
  private val timestamp = "yyyyMMdd"
  private val cvUtils = mock[CVUtils]
  private val underTest =
    TestActorRef(new FileStorage(cvUtils, filePath, timestamp))

  "A FileStorage" can {

    "save image" should {

      "call frame with correct parameters" in {
        // Given
        val frame = mock[CameraFrame]
        // When
        underTest ! Save(frame)
        // Then
        verify(frame).formattedDate(timestamp)
        verify(frame).image
        verifyNoMoreInteractions(frame)
      }

      "call CVUtils with correct parameters" in {
        // Given
        val frame = mock[CameraFrame]
        val image = mock[IplImage]
        val expectedPath = s"$filePath/$timestamp"
        when(frame.formattedDate(timestamp)).thenReturn(timestamp)
        when(frame.image).thenReturn(image)
        // When
        underTest ! Save(frame)
        // Then
        verify(cvUtils).saveImage(expectedPath, image)
        verifyNoMoreInteractions(cvUtils)
      }
    }
  }
}
