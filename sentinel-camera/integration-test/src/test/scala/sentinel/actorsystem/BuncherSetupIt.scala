package sentinel.actorsystem

import akka.testkit.ImplicitSender
import org.scalatest.{AsyncWordSpecLike, Matchers, OneInstancePerTest}
import org.scalatest.mockito.MockitoSugar
import sentinel.app.Buncher
import sentinel.router.Messages
import sentinel.router.Messages.{Finished, Ok, Ready}
import testutils.{BuncherITFixture, StopSystemAfterAll}

import scala.concurrent.Await
import scala.concurrent.duration._


class BuncherSetupIt
    extends BuncherITFixture
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

      start.future map { e =>
        e shouldBe Ready(Ok)
      }
    }

    "happy path2" in {
      val buncher = modules.injector.getInstance(classOf[Buncher])

      val start = buncher.start()

      Await.ready(start.future, 1 seconds)

      val stop = buncher.stop()

      stop.future map { e =>
        e shouldBe Ready(Finished)
      }
    }
  }
}
