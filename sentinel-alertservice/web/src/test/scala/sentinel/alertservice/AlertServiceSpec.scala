package sentinel.alertservice

import java.lang

import com.typesafe.config.Config
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import sentinel.alertservice.database.DataStore
import sentinel.alertservice.database.DataStore.DataStoreOkResponse
import sentinel.alertservice.fixture.AlertMessageFixture
import sentinel.alertservice.server.AlertServiceConfig
import sentinel.app.communication.AlertMessage

import scala.concurrent.Future

class AlertServiceSpec extends AlertServiceServerSpec with AlertMessageFixture with ScalaFutures {

  private val config     = mock[AlertServiceConfig]
  private val dataStore  = mock[DataStore]
  private val response   = "saved"
  private val retryTimes = 10

  before {
    reset(dataStore)
    when(dataStore.save(any[AlertMessage])).thenReturn(Future.successful(DataStoreOkResponse(response)))
  }

  when(config.storageRetryTimes).thenReturn(retryTimes)

  private val alertService = new AlertService(config, dataStore)

  "AlertService" should {

    "call datastore" in {
      val alertMessage = anAlertMessage()

      val result = alertService.save(alertMessage).futureValue

      result shouldBe DataStoreOkResponse(response)
      verify(dataStore).save(alertMessage)
    }

    "retry when underlying fails" in {
      val exception    = new RuntimeException()
      val alertMessage = anAlertMessage()
      reset(dataStore)
      when(dataStore.save(alertMessage))
        .thenReturn(Future.failed(exception),
                    Future.failed(exception),
                    Future.successful(DataStoreOkResponse(response)))


      val result = alertService.save(alertMessage).futureValue

      result shouldBe DataStoreOkResponse(response)
      verify(dataStore, times(3)).save(alertMessage)
    }

    "propagate datastore errors" in {
      val msg          = "error"
      val exception    = new RuntimeException(msg)
      val alertMessage = anAlertMessage()
      reset(dataStore)
      when(dataStore.save(alertMessage)).thenReturn(Future.failed(exception))

      val thrown = intercept[RuntimeException] {
        alertService.save(alertMessage).futureValue
      }

      thrown.getMessage should include(msg)
      verify(dataStore, times(retryTimes+1)).save(alertMessage)
    }
  }
}
