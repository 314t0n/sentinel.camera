package sentinel.camera.camera.module

import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import sentinel.camera.camera.stage.CameraReaderStage
import sentinel.camera.framegrabber.FrameGrabberBuilder
import sentinel.camera.utils.settings.Settings

import scala.concurrent.duration.FiniteDuration
import CameraReaderGraphFactoryProvider._
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory

object CameraReaderGraphFactoryProvider {
  private val oneSecInMillis = 1000
}

@Singleton
class CameraReaderGraphFactoryProvider @Inject()(
    settings: Settings,
    frameGrabberBuilder: FrameGrabberBuilder)
    extends Provider[CameraReaderGraphFactory]
    with LazyLogging {

  private val cameraReaderInstance =
    new CameraReaderGraphFactory(createRawCameraSource, createTickingSource)

  override def get(): CameraReaderGraphFactory = cameraReaderInstance

  private def createRawCameraSource() = {
    logger.debug("Creating grabber")
    val grabber = frameGrabberBuilder.create()
    Source.fromGraph(new CameraReaderStage(grabber))
  }

  private def createTickingSource() = {
    logger.debug("createTickingSource")
    val initialDelay = settings.getDuration("camera.initialDelay", SECONDS)
    val cameraFPS    = settings.getInt("camera.fps")
    val startValue   = 0
    val tickInterval =
      FiniteDuration(oneSecInMillis / cameraFPS, MILLISECONDS)

    Source.tick(initialDelay, tickInterval, startValue)
  }
}
