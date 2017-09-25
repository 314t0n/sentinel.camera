package sentinel.camera.camera.module

import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.google.inject.{Inject, Provider}
import sentinel.camera.camera.module.TickSourceProvider.oneSecInMillis
import sentinel.camera.utils.settings.Settings

import scala.concurrent.duration.FiniteDuration

object TickSourceProvider {
  private val oneSecInMillis = 1000
}

class TickSourceProvider @Inject()(settings: Settings)
    extends Provider[Source[Int, Cancellable]] {

  override def get(): Source[Int, Cancellable] = {

    val initialDelay = settings.getDuration("camera.initialDelay", SECONDS)
    val cameraFPS    = settings.getInt("camera.fps")
    val tickInterval =
      FiniteDuration(oneSecInMillis / cameraFPS, MILLISECONDS)

    Source.tick(initialDelay, tickInterval, 0)
  }

}
