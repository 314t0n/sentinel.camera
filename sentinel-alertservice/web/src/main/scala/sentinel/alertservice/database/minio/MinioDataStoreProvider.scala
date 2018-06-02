package sentinel.alertservice.database.minio

import com.google.inject.Inject
import com.google.inject.Provider
import sentinel.alertservice.server.AlertServiceConfig

class MinioDataStoreProvider @Inject()(minioClientProvider: MinioClientProvider, alertServiceConfig: AlertServiceConfig)
    extends Provider[MinioDataStore] {
  override def get(): MinioDataStore = new MinioDataStore(minioClientProvider, alertServiceConfig.storageBucketId)
}
