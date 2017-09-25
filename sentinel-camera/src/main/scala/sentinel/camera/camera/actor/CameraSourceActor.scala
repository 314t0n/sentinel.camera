package sentinel.camera.camera.actor

import akka.actor.Actor
import com.google.inject.Inject
import sentinel.camera.camera.module.CameraReaderGraphFactory
import sentinel.camera.camera.module.SourceBroadCastFactory
import sentinel.router.Messages._

/**
  * Managing camera source creation
  * @param sourceFactory factory to create camera source instances
  */
class CameraSourceActor @Inject()(sourceFactory: CameraReaderGraphFactory,
                                  broadcastFactory: SourceBroadCastFactory)
    extends Actor {

  override def receive: Receive = {
    case Start(ks) => {
      try {
        val source    = sourceFactory.create(ks)
        val broadcast = broadcastFactory.create(source)
        sender() ! SourceInit(broadcast)
      } catch {
        case t: Throwable => sender() ! Error(t.getMessage)
      }
    }
  }
}
