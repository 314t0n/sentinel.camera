package sentinel.camera.camera.graph

import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph}
import com.typesafe.scalalogging.LazyLogging
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.graph.GraphFactory

class SourceBroadCast(source: CameraSource)
    extends GraphFactory[RunnableGraph[CameraSource]]
    with LazyLogging {

  override def createGraph(): RunnableGraph[CameraSource] =
    source.toMat(BroadcastHub.sink(bufferSize = 1))(Keep.right)
}
