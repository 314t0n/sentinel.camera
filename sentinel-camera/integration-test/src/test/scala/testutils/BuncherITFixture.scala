package testutils

import akka.actor.ActorSystem
import akka.testkit.TestKit
import sentinel.system.module.ModuleInjector

class BuncherITFixture extends TestKit(ActorSystem("IntegrationTestSystem")) {

  protected val modules = new ModuleInjector(system)

}
