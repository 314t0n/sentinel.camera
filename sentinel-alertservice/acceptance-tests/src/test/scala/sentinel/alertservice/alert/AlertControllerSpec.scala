package sentinel.alertservice.alert

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import sentinel.alertservice.AlertService
import sentinel.alertservice.controller.AlertController.AlertMessagePath
import sentinel.alertservice.database.DataStore.DataStoreOkResponse
import sentinel.alertservice.fixture.AlertMessageFixture
import sentinel.app.communication.AlertMessage
import sentinel.server.AlertServiceServer

import scala.concurrent.Future

class AlertControllerSpec extends FeatureTest with BeforeAndAfter with MockitoSugar with AlertMessageFixture {

  private val alertService = mock[AlertService]

  override val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(new AlertServiceServer).bind[AlertService].toInstance(alertService)

  before {
    when(alertService.save(any[AlertMessage])).thenReturn(Future.successful(DataStoreOkResponse("saved")))
  }

  test("save message to remote database") {
    val given = anAlertMessage()

    server.httpPost(
      path = AlertMessagePath,
      contentType = ProtoBufContentType,
      andExpect = Ok,
      postBody = toPostBody(given),
      withBody = "{\"message\":\"saved\"}"
    )

    verify(alertService).save(given)
  }

}
