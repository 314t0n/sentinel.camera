package sentinel.camera.camera.actor

import akka.actor.Actor
import akka.stream.{KillSwitch, KillSwitches}
import sentinel.camera.camera.actor.CameraActor.{Start, Stop}

object CameraActor {

  case class Start(killSwitch: KillSwitch)
  case class Stop()

}

case class CameraState(killSwitch: KillSwitch){

  def start() ={
    () => killSwitch.shutdown()
  }

  def stop: Unit ={
    killSwitch.shutdown()
  }

}


class CameraActor(/* camsourcefactory*/) extends Actor {

  private var killSwitch:KillSwitch = _

  private val state = CameraState(KillSwitches.shared(""))

  override def receive: Receive = {
    case start: Start => {
      sender() ! "ok"
    }
    case Stop => {
      Option(killSwitch).foreach(_.shutdown())

      state.stop

      sender() ! "ok"
    }
  }
}
