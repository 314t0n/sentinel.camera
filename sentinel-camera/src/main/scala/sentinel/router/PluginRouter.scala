package sentinel.router

import akka.actor.{ActorRef, FSM}
import akka.pattern.ask
import akka.routing.{Router, RoutingLogic, SeveralRoutees}
import akka.util.Timeout
import sentinel.router.Messages._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class PluginRouter(cameraSource: ActorRef, routingLogic: RoutingLogic, routees: SeveralRoutees)(
    implicit val ec: ExecutionContext)
    extends FSM[State, Request] {

  private val duration         = 2 seconds
  private implicit val timeout = Timeout(duration) // TODO config

  private val router = Router(routingLogic, routees.routees)

  startWith(Idle, Stop)

  when(Waiting) {
    case Event(GoToActive, _) =>
      goto(Active) using NoRequest

    case Event(GoToIdle, _) =>
      goto(Idle) using NoRequest

    case Event(Ready(Ok), WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToActive
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(Ready(Finished), WaitingForRoutees(requestor, remainingResponses)) =>
      if (remainingResponses == 0) self ! GoToIdle
      stay() using WaitingForRoutees(requestor, remainingResponses - 1)

    case Event(SourceInit(bs), WaitingForSource(sender, Start(ks))) =>
      router.route(PluginStart(ks, bs), self)
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)

    case Event(Ready(Finished), WaitingForSource(sender, Stop)) =>
      router.route(Stop, self)
      goto(Waiting) using WaitingForRoutees(sender, router.routees.size - 1)
  }

  onTransition {
    case Waiting -> Active =>
      stateData match {
        case WaitingForRoutees(requestor, _) => requestor ! Ready(Ok)
      }

    case Waiting -> Idle =>
      stateData match {
        case WaitingForRoutees(requestor, _) => requestor ! Ready(Finished)
      }
  }

  when(Idle) {
    case Event(Start(ks), _) =>
      askSource(Start(ks))
      goto(Waiting) using WaitingForSource(sender, Start(ks))
  }

  private def askSource(request: Request) =
    ask(cameraSource, request)
      .mapTo[Response]
      .onComplete {
        case Success(request: Response) =>
          self ! request
        case Failure(t) =>
          log.error("Error occurred while waiting for response: {}", t)
          self ! GoToIdle
      }

  when(Active) {
    case Event(Stop, _) =>
      askSource(Stop)
      goto(Waiting) using WaitingForSource(sender, Stop)
  }

  whenUnhandled {
    case Event(Start(_), NoRequest) =>
      log.error(AlreadyStarted)
      sender() ! Error(AlreadyStarted)
      stay
    case Event(Stop, Stop) =>
      log.error(Finished)
      sender() ! Error(Finished)
      stay
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}, sender: {}",
                  e,
                  stateName,
                  s,
                  sender)
      stay
  }

  initialize()
}
