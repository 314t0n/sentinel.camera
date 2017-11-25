package sentinel.actorsystem

import akka.testkit.ImplicitSender
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncWordSpecLike, GivenWhenThen, Matchers, OneInstancePerTest}
import sentinel.app.Orchestator
import sentinel.router.messages.Messages.{Finished, Ok}
import sentinel.router.messages.Ready
import testutils.{StartUpFixture, StopSystemAfterAll}

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

//    "receive correct messages without errors" in {
//      val buncher = modules.injector.getInstance(classOf[Orchestator])
//
//      val start = buncher.start()
//
//      Await.ready(start.future, 10 seconds)
//
//      val stop = buncher.stop()
//
//      start.future  map { e =>
//        e shouldBe Ready(Ok)
//      }
//
//      stop.future map { e =>
//        e shouldBe Ready(Finished)
//      }
//    }
  }
}
