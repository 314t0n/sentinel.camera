package sentinel.alertservice.client
import com.google.protobuf.ByteString
import com.twitter.finagle.Http
import com.twitter.finagle.Service
import com.twitter.finagle.http
import com.twitter.io.Buf
import com.twitter.util.Await
import com.twitter.util.Future
import sentinel.app.communication.AlertMessage

object TestM extends App {
  val client = new Client
  try {
    val resp = client.send(new AlertMessage(image = ByteString.copyFromUtf8("test"), timestamp = 1L, cameraId = "test"))
    println(Await.result(resp))
  } finally {
    client.close()
  }
}

class Client() extends AutoCloseable {

  val host = "localhost"
  val port = 8080
  val url  = "/ping"

  val client: Service[http.Request, http.Response] =
    Http.client.newService(s"$host:$port")

  def send(msg: AlertMessage): Future[http.Response] = {
    val request = http.Request(http.Method.Post, url)
    request.contentType = "application/protobuf-x"
    request.content = Buf.ByteArray.Owned(msg.toByteArray)
    client(request)
  }

  override def close(): Unit = client.close()

}
