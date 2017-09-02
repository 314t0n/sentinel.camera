package sentinel

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{BroadcastHub, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import sentinel.Sandbox.{killSwitch, tickingSource}
import sentinel.graph.GraphFactory

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, Future}

object Sandbox extends App with LazyLogging {

  private val timeout = 5000
  //  implicit val ex = scala.concurrent.ExecutionContext.global
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(
        initialSize = 1,
        maxSize = 1))

  import system.dispatcher

  val tickingSource = Source.tick(0.second, 500.millisecond, 0)
  // Shared between flows in order to shutdown the whole graph
  val killSwitch = KillSwitches.shared("switch")

  Await.ready(Future {

    val producer: Source[String, NotUsed] = new SourceGraph().createGraph()

    val fromProducer: Source[String, NotUsed] = new BroadcastingGraph(producer).createGraph().run()

    val processorA = new ProcessingAGraph(fromProducer)
    val processorB = new ProcessingBGraph(fromProducer)

    processorA.createGraph().run()
    processorB.createGraph().run()

  }, FiniteDuration(timeout, TimeUnit.MILLISECONDS))

  Await.ready(Future {
    Thread.sleep(timeout)
    system.terminate()
  }, FiniteDuration(11000, TimeUnit.MILLISECONDS))

}

class SourceGraph() extends GraphFactory[Source[String, NotUsed]] with LazyLogging {

  override def createGraph(): Source[String, NotUsed] =
    Source.fromGraph(GraphDSL.create() {
      implicit builder =>
        import GraphDSL.Implicits._

        val producer = Source(1 to 10)
          .via(killSwitch.flow)
          .zip(tickingSource)
          .map(_._1.toString)

        val ProducerOutlet: Outlet[String] = builder.add(producer).out

        SourceShape(ProducerOutlet.outlet)
    })
}

class BroadcastingGraph(source: Source[String, NotUsed]) extends GraphFactory[RunnableGraph[Source[String, NotUsed]]] with LazyLogging {

  override def createGraph(): RunnableGraph[Source[String, NotUsed]] = source.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)
}

class ProcessingAGraph(source: Source[String, NotUsed]) extends GraphFactory[RunnableGraph[NotUsed]] with LazyLogging {

  override def createGraph(): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create(source) {
      implicit builder =>
        (source) =>
          import GraphDSL.Implicits._

          val processor = Flow[String].via(killSwitch.flow)
            .map(f => s"hey processing $f")

          val display = Flow[String].via(killSwitch.flow)
            .map(f => println(s"hey processing $f"))

          val processorNode: FlowShape[String, String] = builder.add(processor)
          val displayNode: FlowShape[String, Unit] = builder.add(display)
          val Ignore: Inlet[Any] = builder.add(Sink.ignore).in

          source.out ~> processorNode ~> displayNode ~> Ignore

          ClosedShape
    })
}

class ProcessingBGraph(source: Source[String, NotUsed]) extends GraphFactory[RunnableGraph[NotUsed]] with LazyLogging {

  override def createGraph(): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create(source) {
      implicit builder =>
        (source) =>
          import GraphDSL.Implicits._

          val processor = Flow[String].via(killSwitch.flow)
            .map(f => f.toInt)

          val display = Flow[Int].via(killSwitch.flow)
            .map(f => {
              Thread.sleep(f*1000)
              println("wake up")
            })

          val processorNode: FlowShape[String, Int] = builder.add(processor)
          val displayNode: FlowShape[Int, Unit] = builder.add(display)
          val Ignore: Inlet[Any] = builder.add(Sink.ignore).in

          source.out ~> processorNode ~> displayNode ~> Ignore

          ClosedShape
    })
}