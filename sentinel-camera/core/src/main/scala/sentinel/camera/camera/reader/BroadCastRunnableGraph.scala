package sentinel.camera.camera.reader

import java.util.concurrent.TimeoutException

import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{RunnableGraph, Sink}
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource

import scala.concurrent.Future

case class BroadCastRunnableGraph(graph: RunnableGraph[CameraSource])(implicit val materializer: ActorMaterializer) {

  lazy val mat: CameraSource = graph.run()

  @throws[TimeoutException]
  def toFuture(): Future[Done] =
    mat.runWith(Sink.ignore)
}
