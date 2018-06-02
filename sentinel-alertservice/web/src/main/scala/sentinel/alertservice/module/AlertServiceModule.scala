package sentinel.alertservice.module

import com.twitter.inject.TwitterModule

object AlertServiceModule extends TwitterModule {
  override val modules = Seq(DataStoreModule)
}
