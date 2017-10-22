package sentinel.camera.camera.graph.factory

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Keep
import com.google.inject.Inject
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.reader.BroadCastRunnableGraph

class SourceBroadCastFactory @Inject()(
    implicit materializer: ActorMaterializer) {

  def create(source: CameraSource): BroadCastRunnableGraph =
    BroadCastRunnableGraph(
      source.toMat(BroadcastHub.sink(bufferSize = 1))(Keep.right))

}
