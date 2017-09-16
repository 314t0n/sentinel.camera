package sentinel.router

import akka.actor.Actor

class PluginRouter  extends  Actor{
  override def receive: Receive = {
    case _ => sender() ! _
  }
}
