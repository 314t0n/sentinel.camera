package sentinel.camera.camera.module

import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph}
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource

class SourceBroadCastFactory {

  def create(source: CameraSource): RunnableGraph[CameraSource] =
  source.toMat(BroadcastHub.sink(bufferSize = 1))(Keep.right)

}
