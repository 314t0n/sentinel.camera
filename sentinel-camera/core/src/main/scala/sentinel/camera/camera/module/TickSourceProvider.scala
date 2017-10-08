package sentinel.camera.camera.module

import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Provider}
import sentinel.TickSource
import sentinel.camera.camera.module.TickSourceProvider.oneSecInMillis
import sentinel.camera.utils.settings.Settings

import scala.concurrent.duration.FiniteDuration
@deprecated
object TickSourceProvider {
  private val oneSecInMillis = 1000
}
@deprecated
class TickSourceProvider @Inject()(settings: Settings)
    extends Provider[TickSource] {

  override def get(): TickSource = {
    println("What is up dear brother of mine?")
    val initialDelay = settings.getDuration("camera.initialDelay", SECONDS)
    val cameraFPS    = settings.getInt("camera.fps")
    val tickInterval =
      FiniteDuration(oneSecInMillis / cameraFPS, MILLISECONDS)

    Source.tick(initialDelay, tickInterval, 0)
  }

}
