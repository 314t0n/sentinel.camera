package testutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.testkit.TestKit
import com.google.inject.Guice
import com.google.inject.util.Modules
import org.bytedeco.javacv.FrameGrabber
import org.mockito.Mockito.when
import org.scalatest.AsyncWordSpecLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.mockito.MockitoSugar
import sentinel.camera.camera.module.CameraInjector
import sentinel.camera.framegrabber.FrameGrabberBuilder
import sentinel.camera.motiondetector.bgsubtractor.module.BackgroundSubstractorInjector
import sentinel.camera.utils.settings.module.SettingsInjector
import sentinel.router.module.RouterInjector
import sentinel.system.module.SystemInjector
import testutils.StartUpFixture.createInjector
import testutils.modules.TestCameraInjector

object StartUpFixture {

  def createInjector(system: ActorSystem,
                     materializer: ActorMaterializer,
                     frameGrabberBuilder: FrameGrabberBuilder) =
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

class StartUpFixture
    extends TestKit(ActorSystem("IntegrationTestSystem"))
    with AsyncWordSpecLike
    with BeforeAndAfterAll
    with MockitoSugar {

  protected implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withInputBuffer(initialSize = 1, maxSize = 1))

  protected val frameGrabberBuilder = mock[FrameGrabberBuilder]
  protected val grabber             = mock[FrameGrabber]
  protected val injector =
    createInjector(system, materializer, frameGrabberBuilder)

  override def beforeAll() = {
    super.beforeAll()
    when(frameGrabberBuilder.create()).thenReturn(grabber)
  }

}
