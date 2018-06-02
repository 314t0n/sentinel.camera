package sentinel.server.alertservice
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}

object AlertService extends App{
  val service = new Service[http.Request, http.Response] {
    def apply(req: http.Request): Future[http.Response] =
      Future.value(
        http.Response(req.version, http.Status.Ok)
      )
  }
  val server = Http.server.withAdmissionControl
    .concurrencyLimit(
      maxConcurrentRequests = 10,
      maxWaiters = 0
    )
    .serve(":8080", service)
  Await.ready(server)
}
