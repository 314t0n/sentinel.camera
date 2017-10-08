package sentinel.app

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.KillSwitch
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestFSMRef
import akka.testkit.TestKit
import akka.testkit.TestProbe
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest._
import sentinel.camera.utils.settings.Settings
import sentinel.router.Messages._
import testutils.StopSystemAfterAll
import testutils.TestSystem.TestActorSystem

import scala.concurrent.duration._
import scala.util.Try

class BuncherSpec
    extends TestKit(ActorSystem(TestActorSystem))
    with ImplicitSender
    with AsyncWordSpecLike
    with OneInstancePerTest
    with StopSystemAfterAll
    with Matchers
    with MockitoSugar {

  private implicit val ec = system.dispatcher
  private val switch      = TestProbe()
  private val settings    = mock[Settings]
  when(settings.getDuration(any[String], any[TimeUnit]))
    .thenReturn(50 milliseconds)
  private val underTest = new Buncher(switch.ref, settings)

  "Buncher" when {

    "start" should {

      "switch response with ok" in {
        val promise = underTest.start()

        switch.expectMsgAnyClassOf(50 seconds, classOf[Start])
        switch.reply(Ready(Ok))

        promise.future map { e =>
          e shouldBe Ready(Ok)
        }
      }

      "switch response with error" in {
        val promise = underTest.start()
        val reason  = "reason"

        switch.expectMsgAnyClassOf(50 seconds, classOf[Start])
        switch.reply(Error(reason))

        promise.future map { e =>
          e shouldBe Error(reason)
        }
      }
    }

    "stop" should {

      "switch response with finished" in {
        val promise = underTest.start()

        switch.expectMsgAnyClassOf(50 seconds, classOf[Start])
        switch.reply(Ready(Finished))

        promise.future map { e =>
          e shouldBe Ready(Finished)
        }
      }

      "switch response with error" in {
        val promise = underTest.start()
        val reason  = "reason"

        switch.expectMsgAnyClassOf(50 seconds, classOf[Start])
        switch.reply(Error(reason))

        promise.future map { e =>
          e shouldBe Error(reason)
        }
      }
    }
  }
}
