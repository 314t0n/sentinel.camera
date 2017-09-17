package sentinel.router

import akka.actor.ActorRef
import akka.routing.{Routee, Routees}
import akka.stream.KillSwitch

object Messages {

  val Ok = "Ok"
  val Finished = "Finished"
  val AlreadyStarted = "Already started"

  trait Request

  case class Start(ks: KillSwitch) extends Request

  case object NoRequest extends Request

  case object Stop extends Request

  case class WaitingForResponse(requestor: ActorRef, numberOfResponses: Int) extends Request

  private[router] case object GoToActive extends Request

  private[router] case object GoToIdle extends Request

  sealed trait Response

  case class Ready(msg: String) extends Response

  case class Error(reason: String) extends Response

}
