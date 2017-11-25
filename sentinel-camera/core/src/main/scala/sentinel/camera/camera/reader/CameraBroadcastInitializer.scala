package sentinel.camera.camera.reader

import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch

import scala.concurrent.Promise

class CameraBroadcastInitializer @Inject()(
    broadCastMateralizer: BroadcastMateralizer)
    extends LazyLogging {

  def create(gks: GlobalKillSwitch): Promise[BroadCastRunnableGraph] =
    broadCastMateralizer.create(gks)

}
