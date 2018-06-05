package sentinel.alertservice.util

trait Retry[A[_]] {

  /**
    * Interface for retry
    * @param times number of times to retry
    * @param op operation to retry
    * @tparam T type parameter
    * @return
    */
  def retry[T](times: Int)(op: => A[T]): A[T]
}

object Retry {

  def retry[A[_], T](times: Int)(op: => A[T])(implicit retryLogic: Retry[A]): A[T] = retryLogic.retry(times)(op)
}
