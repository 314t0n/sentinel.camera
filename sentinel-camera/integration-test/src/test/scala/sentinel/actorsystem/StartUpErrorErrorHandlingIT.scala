package sentinel.actorsystem

import akka.testkit.ImplicitSender
import org.mockito.Mockito.when
import org.scalatest.AsyncWordSpecLike
import org.scalatest.Matchers
import org.scalatest.OneInstancePerTest
import org.scalatest.mockito.MockitoSugar
import sentinel.app.Orchestator
import sentinel.router.messages._
import testutils.StartUpErrorFixture
import testutils.StopSystemAfterAll

class StartUpErrorErrorHandlingIT
    extends StartUpErrorFixture
    with ImplicitSender
    with AsyncWordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with MockitoSugar {

//  "error handling when IO is not available" in {
//    val buncher = injector.getInstance(classOf[Orchestator])
//
//    when(grabber.start()).thenThrow(
//      new RuntimeException(
//        "avformat_open_input() error -5: Could not open input video=Webcam. (Has setFormat() been called?"))
//
//    val start = buncher.start()
//
//    start.future map { e =>
//      e.asInstanceOf[Error].reason should include("Could not open input")
//    }
//  }
}
