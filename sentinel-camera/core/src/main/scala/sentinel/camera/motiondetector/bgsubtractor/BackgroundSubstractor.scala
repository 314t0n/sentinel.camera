package sentinel.camera.motiondetector.bgsubtractor

import sentinel.camera.camera.{CameraFrame, MotionDetectFrame}

trait BackgroundSubstractor extends AutoCloseable {
  def substractBackground(frame: CameraFrame): MotionDetectFrame
}
