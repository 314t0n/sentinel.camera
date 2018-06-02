package sentinel.alertservice.module

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.TwitterModule
import com.typesafe.config.ConfigFactory
import sentinel.alertservice.database.DataStore
import sentinel.alertservice.database.minio.{MinioClientProvider, MinioDataStoreProvider}
import sentinel.alertservice.server.AlertServiceConfig

object DataStoreModule extends TwitterModule {

  @Singleton
  @Provides
  private val alertServiceConfig = new AlertServiceConfig(ConfigFactory.load())

  @Singleton
  @Provides
  private val minioClientProvider = new MinioClientProvider(alertServiceConfig)

  private val minioDataStoreProvider = new MinioDataStoreProvider(minioClientProvider, alertServiceConfig)

  @Singleton
  @Provides
  def dataStore: DataStore = minioDataStoreProvider.get

}
