package sentinel.router

import akka.actor.ActorRef
import akka.stream.KillSwitch
import akka.stream.scaladsl.RunnableGraph
import sentinel.camera.camera.graph.CameraReaderGraph.CameraSource
import sentinel.camera.camera.graph.SourceBroadCast
import sentinel.camera.camera.reader.BroadCastRunnableGraph

object Messages {

  val Ok             = "Ok"
  val Finished       = "Finished"
  val AlreadyStarted = "Already started"

  trait Request

  case class Start(ks: KillSwitch) extends Request

  case class PluginStart(ks: KillSwitch, broadcast: BroadCastRunnableGraph) extends Request

  case object NoRequest extends Request

  case object Stop extends Request

  case class WaitingForRoutees(requestor: ActorRef, numberOfResponses: Int) extends Request
  case class WaitingForRoutees2(requestor: ActorRef,request: Request, numberOfResponses: Int) extends Request
  case class WaitingForRouter(requestor: ActorRef) extends Request
  case class WaitingForSource(requestor: ActorRef, request: Request) extends Request

  private[router] case object GoToActive extends Request

  private[router] case object GoToIdle extends Request

  sealed trait Response

  case class Ready(msg: String) extends Response

  case class Error(reason: String) extends Response

  case class SourceInit(broadCast: BroadCastRunnableGraph) extends Response

}
