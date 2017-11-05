package sentinel.plugin

import sentinel.router.messages.PluginStart

trait Plugin {

  def start(p: PluginStart)

  def stop()
}
