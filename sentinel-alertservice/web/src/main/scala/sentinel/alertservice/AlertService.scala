package sentinel.alertservice

import com.google.inject.{Provides, Singleton}
import javax.inject.Inject
import sentinel.alertservice.database.DataStore
import sentinel.alertservice.server.AlertServiceConfig
import sentinel.alertservice.util.FutureRetry._
import sentinel.alertservice.util.Retry.retry
import sentinel.app.communication.AlertMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AlertService @Inject()(config: AlertServiceConfig, dataStore: DataStore) {

  private val retryTimes: Int = config.storageRetryTimes

  def save(alertMessage: AlertMessage): Future[DataStore.DataStoreResponse] = {
    storeAlert(alertMessage)
  }

  private def storeAlert(alertMessage: AlertMessage) =
    for {
      dataStoreResult <- retry(retryTimes)(dataStore.save(alertMessage))
    } yield dataStoreResult
}
