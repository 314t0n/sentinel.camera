package sentinel.router

import akka.actor.ActorRef

sealed trait State

case object Idle extends State

case object Waiting extends State

case object Active extends State


