package sentinel.alertservice.database

import sentinel.app.communication.AlertMessage

import scala.concurrent.Future

trait DataStore {

  def save(alertMessage: AlertMessage): Future[String]

}
