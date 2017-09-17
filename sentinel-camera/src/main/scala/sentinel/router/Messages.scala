package sentinel.router

import akka.actor.ActorRef
import akka.stream.KillSwitch
import sentinel.camera.camera.graph.SourceBroadCast

object Messages {

  val Ok             = "Ok"
  val Finished       = "Finished"
  val AlreadyStarted = "Already started"

  trait Request

  case class Start(ks: KillSwitch) extends Request

  case class PluginStart(ks: KillSwitch, bs: SourceBroadCast) extends Request

  case object NoRequest extends Request

  case object Stop extends Request

  case class WaitingForRoutees(requestor: ActorRef, numberOfResponses: Int) extends Request
  case class WaitingForSource(requestor: ActorRef, ks: KillSwitch) extends Request

  private[router] case object GoToActive extends Request

  private[router] case object GoToIdle extends Request

  sealed trait Response

  case class Ready(msg: String) extends Response

  case class Error(reason: String) extends Response

  case class SourceInit(bs: SourceBroadCast) extends Response

}
