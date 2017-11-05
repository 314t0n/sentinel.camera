package sentinel.app

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.KillSwitches
import akka.util.Timeout
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.utils.settings.Settings
import sentinel.plugin.Plugin
import sentinel.router.RouterFSM.Add
import sentinel.router.messages.Request
import sentinel.router.messages.Response
import sentinel.router.messages.Start
import sentinel.router.messages.Stop

import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success

class Buncher @Inject()(@Named("SwitchFSM") switch: ActorRef,
                        @Named("RouterFSM") router: ActorRef,
                        settings: Settings)(
    @Named("MessageExecutionContext") implicit val ec: ExecutionContext)
    extends LazyLogging {

  private val duration =
    settings.getDuration("system.options.startUpTimeout", TimeUnit.SECONDS)
  private implicit val timeout = Timeout(duration)

  def addPlugin(plugin: Plugin) = {
    router ! Add(plugin)
  }

  def start(): Promise[Response] = {
    logger.debug("Start request")
    askSwitch(Start(createKillswitch))
  }

  def stop(): Promise[Response] = {
    logger.debug("Stop request")
    askSwitch(Stop)
  }

  private def askSwitch(request: Request) = {
    val promise = Promise[Response]()
    ask(switch, request)
      .mapTo[Response]
      .onComplete {
        case Success(message) =>
          promise success message
        case Failure(e) =>
          logger.error(s"Error occurred while waiting for response: ${e.getMessage}", e)
          promise failure e
      }
    promise
  }

  private def createKillswitch() = KillSwitches.shared("switch")
}
