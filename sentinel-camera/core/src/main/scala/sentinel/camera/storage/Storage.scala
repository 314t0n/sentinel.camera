package sentinel.camera.storage

import sentinel.camera.camera.CameraFrame

object Storage{
  case class Save(frame: CameraFrame)
}

trait Storage {
  def save(frame: CameraFrame)
}
