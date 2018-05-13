package sentinel.alertservice.controller

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import sentinel.alertservice.controller.AlertController.AlertMessagePath
import sentinel.alertservice.database.DataStore
import sentinel.app.communication.AlertMessage
import scala.concurrent.ExecutionContext.Implicits.global

object AlertController {
  val AlertMessagePath: String = "/alert"
}

class AlertController @Inject()(dataStore: DataStore) extends Controller {
  post(AlertMessagePath) { alertMessage: AlertMessage =>
    for {
      dataStoreResult <- dataStore.save(alertMessage)
    } yield dataStoreResult
  }
}
