package sentinel.alertservice.util
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FutureRetry {

  implicit val futureRetry: Retry[Future] = new Retry[Future] {
    override def retry[T](times: Int)(op: => Future[T]): Future[T] = op.recoverWith {
      case _ if times > 0 => retry(times - 1)(op)
    }
  }
}
