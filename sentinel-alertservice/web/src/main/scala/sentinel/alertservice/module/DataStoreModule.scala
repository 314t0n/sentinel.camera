package sentinel.alertservice.module

import com.google.inject.Provides
import com.google.inject.Singleton
import com.twitter.inject.TwitterModule
import com.typesafe.config.ConfigFactory
import sentinel.alertservice.database.DataStore
import sentinel.alertservice.database.minio.MinioClientProvider
import sentinel.alertservice.database.minio.MinioDataStoreProvider
import sentinel.alertservice.server.AlertServiceConfig

object DataStoreModule extends TwitterModule {

  @Singleton
  @Provides
  private val minioClientProvider = new MinioClientProvider(alertServiceConfig)

  private val minioDataStoreProvider = new MinioDataStoreProvider(minioClientProvider, alertServiceConfig)

  @Singleton
  @Provides
  def alertServiceConfig: AlertServiceConfig = new AlertServiceConfig(ConfigFactory.load())

  @Singleton
  @Provides
  def dataStore: DataStore = minioDataStoreProvider.get()
}
