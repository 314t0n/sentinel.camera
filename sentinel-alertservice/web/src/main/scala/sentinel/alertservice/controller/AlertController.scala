package sentinel.alertservice.controller

import com.twitter.finatra.http.Controller
import sentinel.app.communication.AlertMessage

class AlertController extends Controller {
  post("/alert") { alertMessage: AlertMessage =>
    // todo store alert
    "alert"
  }
}
