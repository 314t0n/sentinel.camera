package sentinel.camera.camera.reader

import java.util.concurrent.TimeoutException

import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.Sink
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource

import scala.concurrent.Future

case class BroadCastRunnableGraph(graph: RunnableGraph[CameraSource])(
    implicit val materializer: ActorMaterializer) {

  @throws[TimeoutException]
  def toFuture(): Future[Done] =
      graph.run().runWith(Sink.ignore)
}
