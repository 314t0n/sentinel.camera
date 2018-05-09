package sentinel.admin

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

class AdminController extends Controller {
  get("/ping") { _: Request =>
    "pong"
  }
}
