package sentinel.camera.camera.actor

import akka.Done
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef
import akka.actor.Props
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.Sink
import akka.stream._
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.graph.CameraReaderGraph
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory
import sentinel.router.Messages._
import sun.reflect.misc.FieldUtil

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

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

  private type BroadCast = RunnableGraph[CameraReaderGraph.CameraSource]
  private final class StartUpException(e: Throwable) extends RuntimeException(e)
  private implicit val executionContext =
    materalizer.system.dispatchers.defaultGlobalDispatcher

  override def receive: Receive = {
    case Start(ks) =>
      Try(createBroadcast(ks, sender())) recover {
        case e: Exception => respondWithError(sender(), e)
      }
  }

  private def createBroadcast(ks: KillSwitch,requestor: ActorRef ) = {
    val reader    = cameraReaderFactory.create(ks)
    val broadcast = broadcastFactory.create(reader)
    val promise   = Promise[Response]()

    promise.future.onComplete {
      case Success(message) => requestor ! message
      case Failure(e)       => respondWithError(requestor, e)
    }

    startBroadcast(broadcast, promise)
  }

  private def startBroadcast(broadcast: BroadCast,
                            promise: Promise[Response]): Unit = {
    val bcastFuture = broadcast
      .run()
      .runWith(Sink.ignore)

    bcastFuture.failed.foreach(e => promise failure e)
    waitForError(broadcast, promise, bcastFuture)
  }

  private def waitForError(broadcast: BroadCast,
                           promise: Promise[Response],
                           bcastFuture: Future[Done]) =
    Try(Await.ready(bcastFuture, 3 seconds)) recover {
      case _: TimeoutException =>
        promise success SourceInit(CameraSourcePublisher(broadcast))
      case e: Exception => promise failure e
    }

  private def respondWithError(requestor: ActorRef, e: Throwable) = {
    log.error("Error occurred while starting source: {}", e)
    requestor ! Error(e.getMessage)
  }
}
