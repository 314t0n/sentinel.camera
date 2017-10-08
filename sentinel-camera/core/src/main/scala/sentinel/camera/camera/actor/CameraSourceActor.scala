package sentinel.camera.camera.actor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory
import sentinel.router.Messages._

object CameraSourceActor {

  def props(cameraReaderFactory: CameraReaderGraphFactory,
            broadcastFactory: SourceBroadCastFactory) = {
    Props(classOf[CameraSourceActor], cameraReaderFactory, broadcastFactory)
  }

}

/**
  * Managing camera source creation
  * @param cameraReaderFactory factory to create camera source instances
  */
class CameraSourceActor(cameraReaderFactory: CameraReaderGraphFactory,
                        broadcastFactory: SourceBroadCastFactory)
    extends Actor
    with ActorLogging {

  override def receive: Receive = {
    case Start(ks) => {
      try {
        log.debug("Start request")
        val reader = cameraReaderFactory.create(ks)
        log.debug("Source created")
        val broadcast = broadcastFactory.create(reader)
        log.debug("Broadcast created")
        sender() ! SourceInit(CameraSourcePublisher(broadcast))
      } catch {
        case e: Exception => {
          log.error("Error occurred while processing request: {}", e)
          sender() ! Error(e.getMessage)
        }
      }
    }
  }
}
