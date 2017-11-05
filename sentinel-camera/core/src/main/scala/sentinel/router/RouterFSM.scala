package sentinel.router

import akka.actor.{ActorSystem, FSM, Props}
import sentinel.camera.utils.settings.Settings
import sentinel.plugin.Plugin
import sentinel.router.RouterFSM.{Add, Remove}
import sentinel.router.messages.Messages.{Finished, Ok}
import sentinel.router.messages._

import scala.util.Try

object RouterFSM {

  val Name = classOf[RouterFSM].getName

  case class Add(plugin: Plugin)

  case class Remove(plugin: Plugin)

  def props(settings: Settings)(implicit system: ActorSystem) = Props(new RouterFSM(settings))

}

class RouterFSM(settings: Settings) extends FSM[State, PluginRouter] {

  startWith(Idle, PluginRouter.empty)

  when(Idle) {
    case Event(Add(plugin), router) =>
      log.debug("Add plugin: {}", plugin)
      stay using router.addPlugin(plugin)

    case Event(Remove(plugin), router) =>
      log.debug("Remove plugin: {}", plugin)
      stay using router.removePlugin(plugin)

    case Event(PluginStart(killSwitch, broadcast), router) =>
      Try({
        val started = router.start(PluginStart(killSwitch, broadcast))
        sender() ! Ready(Ok)
        goto(Active) using started
      }) recover {
        case e: Exception => {
          sender() ! Error(e.getMessage)
          goto(Idle) using router
        }
      } get
  }

  when(Active) {
    case Event(Stop, router) =>
      Try({
        router.stop()
        sender() ! Ready(Finished)
      }) recover {
        case e: Exception => sender() ! Error(e.getMessage)
      }
      goto(Idle) using PluginRouter.empty

    case Event(Add(plugin), router) =>
      log.debug("Add plugin: {}", plugin)
      Try(plugin.start(createPluginStart(router))) recover {
        case e: Exception => {
          sender() ! Error(e.getMessage)
          self ! Remove(plugin)
        }
      }
      stay using router.addPlugin(plugin)

    case Event(Remove(plugin), router) =>
      log.debug("Remove plugin: {}", plugin)
      Try(plugin.stop()) recover { case e: Exception => sender() ! Error(e.getMessage) }
      stay using router.removePlugin(plugin)
  }

  private def createPluginStart(router: PluginRouter) = PluginStart(router.ks.get, router.bs.get)

  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}, sender: {}", e, stateName, s, sender)
      stay
  }

  initialize()
}
