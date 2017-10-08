package sentinel.actorsystem

import akka.testkit.ImplicitSender
import org.scalatest.{AsyncWordSpecLike, Matchers, OneInstancePerTest}
import org.scalatest.mockito.MockitoSugar
import sentinel.app.Buncher
import sentinel.router.Messages.{Ok, Ready}
import testutils.{BuncherITFixture, StopSystemAfterAll}

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
      println("This will be the test")
      val buncher = modules.injector.getInstance(classOf[Buncher])

      val start = buncher.start()

      start.future map { e =>
        e shouldBe Ready(Ok)
      }
    }
  }
}
