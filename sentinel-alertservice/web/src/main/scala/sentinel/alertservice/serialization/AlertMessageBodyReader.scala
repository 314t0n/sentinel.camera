package sentinel.alertservice.serialization

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.finatra.http.marshalling.MessageBodyReader
import sentinel.app.communication.AlertMessage

import scala.language.postfixOps
import scala.reflect._
import scala.util.Try

class AlertMessageBodyReader extends MessageBodyReader[AlertMessage] {

  override def parse[M <: AlertMessage](request: Request)(implicit evidence$1: Manifest[M]): AlertMessage =
    Try {
      AlertMessage.parseFrom(request.getInputStream())
    }.recover {
      case ex: Exception => throw new BadRequestException(ex.getMessage)
    } get
}
