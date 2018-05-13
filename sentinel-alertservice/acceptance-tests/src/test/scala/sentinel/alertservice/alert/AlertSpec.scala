package sentinel.alertservice.alert

import com.twitter.finagle.http.Status.Ok
import sentinel.alertservice.AcceptanceSpec
import sentinel.alertservice.fixture.{AlertMessageFixture, AlertServiceServerFixture}

class AlertSpec extends AcceptanceSpec with AlertServiceServerFixture with AlertMessageFixture {

  test("AlertService can receive protobuf alert message") {
    val testMessage = anAlertMessage()

    sendProtoBuf(alertMessage = testMessage, expectedStatus = Ok, expectedBody = "")
  }
}
