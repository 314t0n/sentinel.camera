package sentinel.alertservice.util

import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import sentinel.alertservice.util.FutureRetry._
import sentinel.alertservice.util.Retry._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class FutureRetrySpec extends WordSpec with MockitoSugar with Matchers with ScalaFutures {

  "FutureRetry" should {

    "call future exactly the given times" in {
      val retryTimes = 10

      var timeCounter = 0
      def mockFuture = Future {
        timeCounter = timeCounter + 1
        retryTimes
      }

      val result = retry(retryTimes)(mockFuture).futureValue

      result shouldBe retryTimes
      timeCounter shouldBe 1
    }

    "retry everytime future fails" in {
      val retryTimes  = 10
      var timeCounter = retryTimes + 1
      def mockFuture = Future {
        timeCounter = timeCounter - 1
        if (timeCounter > 0) throw new RuntimeException
        retryTimes
      }

      val result = retry(retryTimes)(mockFuture).futureValue

      timeCounter shouldBe 0
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

      val result = retry(retryTimes)(mockFuture).futureValue

      timeCounter shouldBe maxRetries
    }
  }
}
