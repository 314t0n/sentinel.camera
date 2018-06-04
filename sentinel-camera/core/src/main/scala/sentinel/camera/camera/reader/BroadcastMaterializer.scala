package sentinel.camera.camera.reader

import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.Sink
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.graph.CameraReaderGraph
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory
import sentinel.camera.camera.reader.BroadcastMaterializer.StreamClosedError
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch
import sentinel.camera.utils.settings.Settings

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object BroadcastMaterializer {
  val StreamClosedError = "Stream unexpectedly stopped."
  type BroadCast = RunnableGraph[CameraReaderGraph.CameraSource]
}

class BroadcastMaterializer @Inject()(
    @Named("CameraReaderFactory") cameraReaderFactory: CameraReaderGraphFactory,
    broadcastFactory: SourceBroadCastFactory,
    settings: Settings)(implicit val materializer: ActorMaterializer) extends LazyLogging{

  private implicit val executionContext =
    materializer.system.dispatchers.defaultGlobalDispatcher

  def create(gks: GlobalKillSwitch): Promise[BroadCastRunnableGraph] = {
    val reader    = cameraReaderFactory.create(gks)
    val broadcast = broadcastFactory.create(reader)
    val promise   = Promise[BroadCastRunnableGraph]()
    materalize(broadcast, promise)

    promise
  }

  private def materalize(broadcast: BroadCastRunnableGraph, promise: Promise[BroadCastRunnableGraph]) =
//    awaitBroadcastStartUp(broadcast, promise)
//    promise success broadcast
    Try(awaitBroadcastStartUp(broadcast, promise)) recover {
      case e: TimeoutException => promise failure e
      case e: Exception        => promise failure e
    }

  private def awaitBroadcastStartUp(broadcast: BroadCastRunnableGraph, promise: Promise[BroadCastRunnableGraph]) = {
    val broadcastStream = broadcast.mat.take(1).runWith(Sink.foreach(f =>{
      println(f)
    }))

    broadcastStream.onComplete {
      case Success(Done) => promise success broadcast
//        promise failure new RuntimeException(StreamClosedError)
      case Failure(e) => {
        logger.error("-----------")
        logger.error(e.getMessage,e)
        promise failure e
      }
    }

//    Await.ready(broadcastStream, 2 seconds)
  }
}