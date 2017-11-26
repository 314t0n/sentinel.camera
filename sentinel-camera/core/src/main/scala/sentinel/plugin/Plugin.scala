package sentinel.plugin

import sentinel.router.messages.AdvancedPluginStart

trait Plugin {

  def start(p: AdvancedPluginStart)

  def stop()
}
