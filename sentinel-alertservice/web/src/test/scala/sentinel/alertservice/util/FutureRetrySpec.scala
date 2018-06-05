package sentinel.alertservice.util

import org.scalatest.AsyncWordSpec
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import sentinel.alertservice.util.FutureRetry._
import sentinel.alertservice.util.Retry._

import scala.concurrent.Future

class FutureRetrySpec extends AsyncWordSpec with MockitoSugar with Matchers {

  "FutureRetry" should {

    "call future exactly the given times" in {
      val retryTimes  = 10
      var timeCounter = 0
      def mockFuture = Future {
        timeCounter = timeCounter + 1
        retryTimes
      }

      val futureRes = retry(retryTimes)(mockFuture)

      futureRes.map(result => {
        result shouldBe retryTimes
        timeCounter shouldBe 1
      })
    }

    "retry everytime future fails" in {
      val retryTimes  = 10
      var timeCounter = retryTimes
      def mockFuture = Future {
        timeCounter = timeCounter - 1
        if (timeCounter > 0) throw new RuntimeException
        retryTimes
      }

      val futureRes = retry(retryTimes)(mockFuture)

      futureRes.map(_ => timeCounter shouldBe 0)
    }

    "short circuit" in {
      val retryTimes  = 10
      val maxRetries  = 5
      var timeCounter = retryTimes
      def mockFuture = Future {
        timeCounter = timeCounter - 1
        if (timeCounter > maxRetries) throw new RuntimeException
        retryTimes
      }

      val futureRes = retry(retryTimes)(mockFuture)

      futureRes.map(_ => timeCounter shouldBe maxRetries)
    }
  }
}
