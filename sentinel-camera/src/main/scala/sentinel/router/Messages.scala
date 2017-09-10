package sentinel.router

import akka.stream.KillSwitch

object Messages {

  val Ok = "Ok"
  val Finished = "Finished"
  val AlreadyStarted = "Already started"

  sealed trait Request

  case class Start(ks: KillSwitch) extends Request

  case object Stop extends Request

  sealed trait Response

  case class Ready(msg: String) extends Response

  case class Error(reason: String) extends Response

}
