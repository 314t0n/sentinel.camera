package sentinel.alertservice.fixture

import com.twitter.finagle.http.{Response, Status}
import sentinel.alertservice.AcceptanceSpec
import sentinel.alertservice.controller.AlertController.AlertMessagePath
import sentinel.app.communication.AlertMessage

trait AlertServiceServerFixture extends AlertMessageFixture{

  this: AcceptanceSpec =>

  def sendProtoBuf(alertMessage: AlertMessage, expectedStatus: Status, expectedBody: String): Response = {
    server.httpPost(
      path = AlertMessagePath,
      contentType = ProtoBufContentType,
      andExpect = expectedStatus,
      postBody = toPostBody(alertMessage),
      withBody = expectedBody
    )
  }
}
