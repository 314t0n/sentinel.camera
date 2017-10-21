package testutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.testkit.TestKit
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.util.Modules
import org.bytedeco.javacv.FrameGrabber
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.module.CameraInjector
import sentinel.camera.framegrabber.FrameGrabberBuilder
import sentinel.camera.motiondetector.bgsubtractor.module.BackgroundSubstractorInjector
import sentinel.camera.utils.settings.module.SettingsInjector
import sentinel.router.module.RouterInjector
import sentinel.system.module.SystemInjector
import testutils.StartUpErrorFixture.createInjector
import testutils.modules.TestCameraInjector

object StartUpErrorFixture {

  def createInjector(system: ActorSystem,
                     materializer: ActorMaterializer,
                     frameGrabberBuilder: FrameGrabberBuilder): Injector =
    Guice.createInjector(
      new SystemInjector(system, materializer),
      new SettingsInjector(),
      new BackgroundSubstractorInjector(),
      Modules
        .`override`(new CameraInjector())
        .`with`(new TestCameraInjector(frameGrabberBuilder)),
      new RouterInjector()
    )
}

class StartUpErrorFixture
    extends TestKit(ActorSystem("IntegrationTestSystem"))
    with MockitoSugar {

  protected implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(initialSize = 1, maxSize = 1))

  protected val frameGrabberBuilder = mock[FrameGrabberBuilder]
  protected val grabber             = mock[FrameGrabber]

  when(frameGrabberBuilder.create()).thenReturn(grabber)
  protected val injector: Injector =
    createInjector(system, materializer, frameGrabberBuilder)

}
