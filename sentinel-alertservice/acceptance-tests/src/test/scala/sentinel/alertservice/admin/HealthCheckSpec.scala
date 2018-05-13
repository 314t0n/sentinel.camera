package sentinel.alertservice.admin
import com.twitter.finagle.http.Status._
import sentinel.alertservice.AcceptanceSpec

class HealthCheckSpec extends AcceptanceSpec {

  test("Server#ping works") {
    server.httpGet(path = "/ping", andExpect = Ok, withBody = "pong")
  }

  test("Server#startup") {
    server.assertHealthy()
  }
}
