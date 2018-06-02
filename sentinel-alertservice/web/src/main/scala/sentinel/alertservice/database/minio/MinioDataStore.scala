package sentinel.alertservice.database.minio

import java.io.ByteArrayInputStream

import io.minio.MinioClient
import sentinel.alertservice.database.DataStore
import sentinel.alertservice.database.DataStore.{DataStoreErrorResponse, DataStoreOkResponse}
import sentinel.alertservice.database.minio.MinioDataStore.JPEG
import sentinel.app.communication.AlertMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Try

object MinioDataStore {
  val JPEG: String = "image/jpg"
}

/**
  * Stores alert's in Minio object storage server
  * @param minioClientProvider provides a new instace of minio client to communicate with minio
  * @param bucketId bucket id to store images
  */
class MinioDataStore(minioClientProvider: MinioClientProvider, bucketId: String) extends DataStore {

  override def save(alertMessage: AlertMessage): Future[DataStore.DataStoreResponse] = Future {
    Try {
      storeImage(alertMessage)
      DataStoreOkResponse(s"Image with name ${imageName(alertMessage)} saved.")
    } recover {
      case e: Exception => DataStoreErrorResponse(e)
    } get
  }

  private def imageName(alertMessage: AlertMessage) = s"${alertMessage.cameraId}_${alertMessage.timestamp}"

  private def storeImage(alertMessage: AlertMessage): Unit = {
    val imageInputStream = new ByteArrayInputStream(alertMessage.toByteArray)
    minioClientProvider.get().putObject(bucketId, imageName(alertMessage), imageInputStream, JPEG)
  }
}
