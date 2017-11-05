package sentinel.camera.camera.reader

import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.KillSwitch
import akka.stream.scaladsl.{RunnableGraph, Sink}
import com.google.inject.Inject
import com.google.inject.name.Named
import sentinel.camera.camera.graph.CameraReaderGraph
import sentinel.camera.camera.graph.factory.CameraReaderGraphFactory
import sentinel.camera.camera.graph.factory.SourceBroadCastFactory
import sentinel.camera.camera.reader.BroadcastMateralizer.StreamClosedError
import sentinel.camera.utils.settings.Settings

import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

object BroadcastMateralizer {
  val StreamClosedError = "Stream unexpectedly stopped."
  type BroadCast = RunnableGraph[CameraReaderGraph.CameraSource]
}

class BroadcastMateralizer @Inject()(
    @Named("CameraReaderFactory") cameraReaderFactory: CameraReaderGraphFactory,
    broadcastFactory: SourceBroadCastFactory,
    settings: Settings)(implicit val materializer: ActorMaterializer) {

  private implicit val executionContext =
    materializer.system.dispatchers.defaultGlobalDispatcher

  def create(ks: KillSwitch) = {
    val reader    = cameraReaderFactory.create(ks)
    val broadcast = broadcastFactory.create(reader)
    val promise   = Promise[BroadCastRunnableGraph]()
    materalize(broadcast, promise)

    promise
  }

  private def materalize(broadcast: BroadCastRunnableGraph,
                         promise: Promise[BroadCastRunnableGraph]) =
    Try(awaitBroadcastStartUp(broadcast, promise)) recover {
      case _: TimeoutException => promise success broadcast
      case e: Exception        => promise failure e
    }

  private def awaitBroadcastStartUp(broadcast: BroadCastRunnableGraph,
                          promise: Promise[BroadCastRunnableGraph]) = {
    val broadcastStream = broadcast.mat.runWith(Sink.ignore)

    broadcastStream.onComplete {
      case Success(Done) =>
        promise failure new RuntimeException(StreamClosedError)
      case Failure(e) => promise failure e
    }

    Await.ready(broadcastStream, 3 seconds)
  }
}
