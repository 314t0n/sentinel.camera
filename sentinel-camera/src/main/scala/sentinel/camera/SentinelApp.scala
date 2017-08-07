package sentinel.camera

import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.config.ModuleInjector
import sentinel.camera.utils.webcam.WebcamWindow

object SentinelApp extends App with LazyLogging {

  val Version = 2.0

  logger.trace(s"Sentinel v$Version is starting ...")

  val modules = ModuleInjector()

  modules.injector.getInstance(classOf[WebcamWindow]).start()

  logger.trace(s"Sentinel v$Version has started.")
}
