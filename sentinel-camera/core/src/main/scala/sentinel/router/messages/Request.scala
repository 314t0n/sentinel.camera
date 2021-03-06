package sentinel.router.messages

import akka.actor.ActorRef
import akka.stream.KillSwitch
import sentinel.camera.camera.reader.BroadCastRunnableGraph

sealed trait Request

case object NoRequest extends Request

case object Stop extends Request

case object Started extends Request

case class Start(ks: KillSwitch) extends Request

case class PluginStart(ks: KillSwitch, broadcast: BroadCastRunnableGraph) extends Request

case class WaitingForRoutees(requestor: ActorRef, numberOfResponses: Int) extends Request

case class WaitingForRouter(requestor: ActorRef) extends Request

case class WaitingForSource(requestor: ActorRef, request: Request) extends Request

private[router] case object GoToActive extends Request

private[router] case object GoToIdle extends Request

private[router] case object RouterTimeouted extends Request
