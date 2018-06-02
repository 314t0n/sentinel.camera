package sentinel.alertservice.fixture

import com.google.protobuf.ByteString
import com.twitter.io.Buf
import sentinel.app.communication.AlertMessage

trait AlertMessageFixture {

  val ProtoBufContentType: String = "application/x-protobuf"

  def bufToString(buf: Buf): String = {
    val Buf.Utf8(str) = buf
    str
  }

  def toPostBody(msg: AlertMessage): String =
    bufToString(Buf.ByteArray.Owned(msg.toByteArray))

  def anAlertMessage(image: ByteString = ByteString.copyFromUtf8("test"),
                     timestamp: Long = 1L,
                     cameraId: String = "test"): AlertMessage =
    new AlertMessage(image = image, timestamp = timestamp, cameraId = cameraId)

  def alertMessageConverter(alertMessage: AlertMessage): String = Buf.ByteArray.Owned(alertMessage.toByteArray).toString

}
