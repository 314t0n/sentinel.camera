package sentinel.camera.camera.module

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import akka.stream.{KillSwitch, SharedKillSwitch}
import com.google.inject.Inject
import org.bytedeco.javacv.Frame
import sentinel.camera.camera.graph.CameraReaderGraph
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource

/**
  * Class for creating CameraSource instances
  * @param cameraSource akka-stream source
  * @param tickingSource akka-stream tick source
  */
class CameraReaderGraphFactory @Inject()(cameraSource: Source[Frame, NotUsed],
                                         tickingSource: Source[Int, Cancellable]) {

  /**
    * Creates a new CameraSource instance
    * @param killSwitch A SharedKillswitch to stop the source and as well as the whole stream
    * @return a new CameraSource instance
    */
  def create(killSwitch: KillSwitch): CameraSource =
    new CameraReaderGraph(cameraSource, tickingSource, killSwitch.asInstanceOf[SharedKillSwitch]).createGraph()

}
