package sentinel.camera.camera.actor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.stream.scaladsl.Sink
import akka.stream.ActorAttributes
import akka.stream.ActorMaterializer
import akka.stream.SharedKillSwitch
import akka.stream.Supervision
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory
import sentinel.router.Messages._
import sun.reflect.misc.FieldUtil

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

object CameraSourceActor {

  val Name = classOf[CameraSourceActor].getName

  def props(cameraReaderFactory: CameraReaderGraphFactory,
            broadcastFactory: SourceBroadCastFactory,
            materalizer: ActorMaterializer) = {
    Props(classOf[CameraSourceActor],
          cameraReaderFactory,
          broadcastFactory,
          materalizer)
  }

}

/**
  * Managing camera source creation
  *
  * @param cameraReaderFactory factory to create camera source instances
  */
class CameraSourceActor(cameraReaderFactory: CameraReaderGraphFactory,
                        broadcastFactory: SourceBroadCastFactory,
                        implicit val materalizer: ActorMaterializer)
    extends Actor
    with ActorLogging
    with LazyLogging {

  implicit val executionContext =
    materalizer.system.dispatchers.defaultGlobalDispatcher

  override def receive: Receive = {
    case Start(ks) => {
      try {

        log.debug("Start request")
        val reader = cameraReaderFactory.create(ks)
        log.debug("Source created")
        val broadcast = broadcastFactory.create(reader)

        val brodcast = broadcast
          .run()
          .runWith(Sink.ignore)

        brodcast.onComplete {
          case Success(v) => {
            log.debug("Broadcast created")
            sender() ! SourceInit(CameraSourcePublisher(broadcast))
          }
          case Failure(e) => {
            log.error("Error occurred while starting source: {}", e)
            sender() ! Error(e.getMessage)
          }
        }

        Await.result(brodcast, 5 seconds)

      } catch {
        case e: Exception => {
          log.error("Error occurred while processing request: {}", e)
          sender() ! Error(e.getMessage)
        }
      }
    }
  }
}
