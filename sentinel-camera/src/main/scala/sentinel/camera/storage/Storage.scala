package sentinel.camera.storage

import sentinel.camera.webcam.CameraFrame

object Storage{
  case class Save(frame: CameraFrame)
}

trait Storage {
  def save(frame: CameraFrame)
}
