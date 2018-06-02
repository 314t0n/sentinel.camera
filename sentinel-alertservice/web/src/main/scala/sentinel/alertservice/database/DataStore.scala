package sentinel.alertservice.database

import sentinel.alertservice.database.DataStore.DataStoreResponse
import sentinel.app.communication.AlertMessage

import scala.concurrent.Future

object DataStore {
  sealed class DataStoreResponse()

  object DataStoreOkResponse {
    def apply(message: String): DataStoreOkResponse = DataStoreOkResponse(Some(message))
  }
  case class DataStoreOkResponse(message: Option[String]) extends DataStoreResponse

  case class DataStoreErrorResponse(exception: Exception) extends DataStoreResponse
}

trait DataStore {

  def save(alertMessage: AlertMessage): Future[DataStoreResponse]

}
