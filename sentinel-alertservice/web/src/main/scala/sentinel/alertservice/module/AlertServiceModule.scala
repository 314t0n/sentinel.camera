package sentinel.alertservice.module

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.inject.TwitterModule
import sentinel.alertservice.AlertService
import sentinel.alertservice.database.DataStore
import sentinel.alertservice.server.AlertServiceConfig

object AlertServiceModule extends TwitterModule {
  override val modules = Seq(DataStoreModule)

  @Singleton
  @Provides
  def alertServiceProvider(config: AlertServiceConfig, dataStore: DataStore): AlertService =
    new AlertService(config, dataStore)
}
