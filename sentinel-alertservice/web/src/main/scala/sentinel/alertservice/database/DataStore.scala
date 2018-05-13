package sentinel.alertservice.database

import com.google.inject.ImplementedBy
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

@ImplementedBy(classOf[DummyDataStore])
trait DataStore {

  def save(alertMessage: AlertMessage): Future[DataStoreResponse]

}
