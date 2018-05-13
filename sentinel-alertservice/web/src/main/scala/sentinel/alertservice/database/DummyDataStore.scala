package sentinel.alertservice.database
import sentinel.alertservice.database.DataStore.{DataStoreOkResponse, DataStoreResponse}
import sentinel.app.communication.AlertMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DummyDataStore extends DataStore {
  override def save(alertMessage: AlertMessage): Future[DataStoreResponse] =
    Future { DataStoreOkResponse("Dummy save") }
}
