package sentinel.server

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import sentinel.admin.AdminController
import sentinel.alertservice.controller.AlertController
import sentinel.alertservice.module.AlertServiceModule
import sentinel.alertservice.serialization.AlertMessageBodyReader
import sentinel.app.communication.AlertMessage

class AlertServiceServer() extends HttpServer {

  override val modules = Seq(AlertServiceModule)

  override val defaultFinatraHttpPort: String = ":8080"

  override protected def configureHttp(router: HttpRouter): Unit =
    router
      .register[AlertMessageBodyReader, AlertMessage]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[AdminController]
      .add[AlertController]
}
