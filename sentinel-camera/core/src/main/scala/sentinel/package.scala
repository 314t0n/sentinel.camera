import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import org.bytedeco.javacv.Frame

package object sentinel {
  type TickSource = Source[Int, Cancellable]
}
