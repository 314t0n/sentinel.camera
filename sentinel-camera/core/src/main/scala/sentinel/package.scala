import akka.actor.Cancellable
import akka.stream.scaladsl.Source

package object sentinel {
  type TickSource = Source[Int, Cancellable]
}
