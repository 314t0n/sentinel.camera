package sentinel.alertservice

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest
import sentinel.server.AlertServiceServer

abstract class AcceptanceSpec extends FeatureTest {

  override val server = new EmbeddedHttpServer(new AlertServiceServer)
}
