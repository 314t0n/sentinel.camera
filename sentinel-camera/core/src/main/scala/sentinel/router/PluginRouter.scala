package sentinel.router

import sentinel.camera.camera.reader.BroadCastRunnableGraph
import sentinel.camera.camera.reader.KillSwitches.GlobalKillSwitch
import sentinel.plugin.Plugin
import sentinel.router.messages.AdvancedPluginStart

object PluginRouter {
  def empty(): PluginRouter = PluginRouter(Seq.empty, None, None)
}

/**
  * Routing start/stop messages to Plugins
  *
  * @param plugins
  * @param ks
  * @param bs
  */
case class PluginRouter(plugins: Seq[Plugin], ks: Option[GlobalKillSwitch], bs: Option[BroadCastRunnableGraph]) {

  def addPlugin(plugin: Plugin): PluginRouter =
    if (plugins.contains(plugin)) this
    else PluginRouter(plugins :+ plugin, ks, bs)

  def removePlugin(plugin: Plugin): PluginRouter = PluginRouter(plugins diff Seq(plugin), ks, bs)

  def start(ps: AdvancedPluginStart): PluginRouter = {
    plugins.foreach(_.start(ps))
    PluginRouter(plugins, Some(ps.ks), Some(ps.broadcast))
  }

  def stop(): PluginRouter = {
    plugins.foreach(_.stop())
    PluginRouter(plugins, None, None)
  }

}
