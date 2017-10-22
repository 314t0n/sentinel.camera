package sentinel.camera.camera.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream._
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.reader.BroadcastMateralizer
import sentinel.router.Messages._

import scala.util.{Failure, Success, Try}

object CameraSourceActor {
  val Name = classOf[CameraSourceActor].getName

  def props(broadCastMateralizer: BroadcastMateralizer,
            materalizer: ActorMaterializer) = {
    Props(new CameraSourceActor(broadCastMateralizer)(materalizer))
  }
}

/**
  * Managing camera source creation
  *
  * @param broadCastMateralizer factory to materalize broadcast stream from source
  */
class CameraSourceActor(broadCastMateralizer: BroadcastMateralizer)(
    implicit val materalizer: ActorMaterializer)
    extends Actor
    with ActorLogging
    with LazyLogging {

  private implicit val executionContext =
    materalizer.system.dispatchers.defaultGlobalDispatcher

  override def receive: Receive = {
    case Start(ks) =>
      val requestor = sender()
      Try(broadCastMateralizer.create(ks).future.onComplete {
        case Success(bs) => requestor ! SourceInit(bs)
        case Failure(e)  => respondWithError(requestor, e)
      }) recover {
        case e: Exception => respondWithError(requestor, e)
      }
  }

  private def respondWithError(requestor: ActorRef, e: Throwable) = {
    log.error("Error occurred while starting source: {}", e)
    requestor ! Error(e.getMessage)
  }
}
