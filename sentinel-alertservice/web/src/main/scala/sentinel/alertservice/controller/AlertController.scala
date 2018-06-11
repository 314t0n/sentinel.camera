package sentinel.alertservice.controller

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import sentinel.alertservice.AlertService
import sentinel.alertservice.controller.AlertController.AlertMessagePath
import sentinel.app.communication.AlertMessage

import scala.concurrent.ExecutionContext.Implicits.global

object AlertController {
  val AlertMessagePath: String = "/alert"
}

class AlertController @Inject()(alertService: AlertService) extends Controller {
  post(AlertMessagePath) { alertMessage: AlertMessage =>
    for {
      response <- alertService.save(alertMessage)
    } yield response
  }
}
