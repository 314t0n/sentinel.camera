package sentinel.camera.camera.reader

import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch

import scala.concurrent.Promise

class CameraReaderFactory @Inject()(broadCastMateralizer: BroadcastMateralizer)(implicit val materalizer: ActorMaterializer)
  extends LazyLogging {

  def create(gks: GlobalKillSwitch): Promise[BroadCastRunnableGraph] = {
    // + send msg to plugin registry
    broadCastMateralizer.create(gks)
  }

}
