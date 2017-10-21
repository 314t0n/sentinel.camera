package testutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.testkit.TestKit
import sentinel.system.module.ModuleInjector

class StartUpFixture extends TestKit(ActorSystem("IntegrationTestSystem")) {
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(initialSize = 1, maxSize = 1))

  protected val modules = new ModuleInjector(system, materializer)

}
