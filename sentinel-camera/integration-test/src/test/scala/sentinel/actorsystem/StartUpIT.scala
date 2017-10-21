package sentinel.actorsystem

import akka.testkit.ImplicitSender
import org.scalatest.AsyncWordSpecLike
import org.scalatest.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.mockito.MockitoSugar
import sentinel.app.Buncher
import sentinel.router.Messages.Finished
import sentinel.router.Messages.Ready
import testutils.StartUpFixture
import testutils.StopSystemAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class StartUpIT
    extends StartUpFixture
    with ImplicitSender
    with AsyncWordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with MockitoSugar {

  "Start" should {

    "happy path" in {
      val buncher = modules.injector.getInstance(classOf[Buncher])

      val start = buncher.start()

      Await.ready(start.future, 10 seconds)

      val stop = buncher.stop()

      stop.future map { e =>
        e shouldBe Ready(Finished)
      }
    }
  }
}
