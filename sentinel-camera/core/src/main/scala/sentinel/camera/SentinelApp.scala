package sentinel.camera

import com.typesafe.scalalogging.LazyLogging

object SentinelApp extends App with LazyLogging {

  val Version = 2.0

  logger.trace(s"Sentinel v$Version is starting ...")

//  val modules = ModuleInjector()

  //  modules.injector.getInstance(classOf[WebcamWindow]).start()

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      System.out.println("Shutdown hook ran!")
    }
  })

  Thread.sleep(1000000000)


  logger.trace(s"Sentinel v$Version has started.")
}
