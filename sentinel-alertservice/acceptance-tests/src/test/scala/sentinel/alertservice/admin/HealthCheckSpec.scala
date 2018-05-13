package sentinel.alertservice.admin

import com.twitter.finagle.http.Status._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import sentinel.server.AlertServiceServer

class HealthCheckSpec extends FeatureTest {

  override val server = new EmbeddedHttpServer(new AlertServiceServer)

  test("ping works") {
    server.httpGet(path = "/ping", andExpect = Ok, withBody = "pong")
  }
}
