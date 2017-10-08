package sentinel.camera.storage

import akka.actor.Actor
import sentinel.camera.storage.Storage._
import sentinel.camera.utils.CVUtils
import sentinel.camera.camera.CameraFrame

class FileStorage(cvUtils: CVUtils,
                  filePath: String = ".",
                  timestamp: String = "yyyy_MM_dd__HH_mm_ss.SS")
  extends Actor with Storage {

  override def receive: Receive = {
    case Save(frame) => save(frame)
  }

  override def save(frame: CameraFrame): Unit =
    cvUtils.saveImage(s"$filePath/${frame.formattedDate(timestamp)}", frame.image)
}
