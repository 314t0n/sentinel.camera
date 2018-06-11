package sentinel.alertservice.database

import java.io.InputStream

import io.minio.MinioClient
import org.mockito.Matchers.any
import org.mockito.Matchers.{eq => is}
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import sentinel.alertservice.AlertServiceServerSpec
import sentinel.alertservice.database.DataStore.DataStoreErrorResponse
import sentinel.alertservice.database.DataStore.DataStoreOkResponse
import sentinel.alertservice.database.minio.MinioClientProvider
import sentinel.alertservice.database.minio.MinioDataStore
import sentinel.alertservice.database.minio.MinioDataStore.JPEG
import sentinel.alertservice.fixture.AlertMessageFixture

class MinioDataStoreServerSpec extends AlertServiceServerSpec with ScalaFutures with AlertMessageFixture {

  private val minioClientProvider = mock[MinioClientProvider]
  private val minioClient         = mock[MinioClient]
  private val bucketId            = "testbucket"
  private val underTest           = new MinioDataStore(minioClientProvider, bucketId)

  before {
    reset(minioClient, minioClientProvider)
    when(minioClientProvider.get()).thenReturn(minioClient)
  }

  "MinioDataStore" should {

    "responds with Ok" in {
      val message      = anAlertMessage()
      val expectedName = s"${message.cameraId}_${message.timestamp}"

      val result = underTest.save(message).futureValue
      result shouldBe DataStoreOkResponse(s"Image with name $expectedName saved.")

      verify(minioClient).putObject(is(bucketId), is(expectedName), any[InputStream], is(JPEG))
    }

    "responds with Error when MinioClient throws exception" in {
      val message      = anAlertMessage()
      val expectedName = s"${message.cameraId}_${message.timestamp}"
      val expectedEx   = new RuntimeException("Error")
      when(minioClient.putObject(is(bucketId), is(expectedName), any[InputStream], is(JPEG)))
        .thenThrow(expectedEx)

      val result = underTest.save(message).futureValue
      result shouldBe DataStoreErrorResponse(expectedEx)

      verify(minioClient).putObject(is(bucketId), is(expectedName), any[InputStream], is(JPEG))
    }
  }
}
