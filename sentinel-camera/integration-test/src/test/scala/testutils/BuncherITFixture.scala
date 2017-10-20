package testutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.testkit.TestKit
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.util.Modules
import org.bytedeco.javacv.FrameGrabber
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.module.CameraInjector
import sentinel.camera.framegrabber.FFmpegFrameGrabberBuilder
import sentinel.camera.framegrabber.FrameGrabberBuilder
import sentinel.camera.motiondetector.bgsubtractor.module.BackgroundSubstractorInjector
import sentinel.camera.utils.settings.module.SettingsInjector
import sentinel.router.module.RouterInjector
import sentinel.system.module.ModuleInjector
import sentinel.system.module.SystemInjector

class BuncherITFixture
    extends TestKit(ActorSystem("IntegrationTestSystem"))
    with MockitoSugar {

  val frameGrabberBuilder = mock[FrameGrabberBuilder]
  val grabber = mock[FrameGrabber]
  when(frameGrabberBuilder.create()).thenReturn(grabber)

  class TestCameraInjector(fgBuilder: FrameGrabberBuilder)
      extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[FrameGrabberBuilder]).toInstance(fgBuilder)
    }
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(initialSize = 1, maxSize = 1))

//  protected val modules = new ModuleInjector(system, materializer)

  val injector = Guice.createInjector(
    new SystemInjector(system, materializer),
    new SettingsInjector(),
    new BackgroundSubstractorInjector(),
    Modules
      .`override`(new CameraInjector())
      .`with`(new TestCameraInjector(frameGrabberBuilder)),
    new RouterInjector()
  )

}
