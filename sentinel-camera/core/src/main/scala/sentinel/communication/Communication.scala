package sentinel.communication

import sentinel.camera.camera.CameraFrame

trait Communication {

  def send(msg: CameraFrame): Either[String, String]
  def sendBatch(msg: Seq[CameraFrame]): Either[String, String]

}
