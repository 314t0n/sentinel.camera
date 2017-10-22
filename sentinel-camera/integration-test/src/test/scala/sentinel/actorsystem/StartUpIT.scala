package sentinel.actorsystem

import akka.testkit.ImplicitSender
import org.scalatest.AsyncWordSpecLike
import org.scalatest.GivenWhenThen
import org.scalatest.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.mockito.MockitoSugar
import sentinel.app.Buncher
import sentinel.router.Messages
import sentinel.router.Messages.{Finished, Ok, Ready}
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
    with GivenWhenThen
    with MockitoSugar {

  Given("Camera is connected")

  "Start and Stop command" should {

    "receive correct messages without errors" in {
      val buncher = modules.injector.getInstance(classOf[Buncher])

      val start = buncher.start()

      Await.ready(start.future, 10 seconds)

      val stop = buncher.stop()

      start.future  map { e =>
        e shouldBe Ready(Ok)
      }

      stop.future map { e =>
        e shouldBe Ready(Finished)
      }
    }
  }
}
