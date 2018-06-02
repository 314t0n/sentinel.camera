package sentinel.alertservice.database.minio

import com.google.inject.{Inject, Provider}
import io.minio.MinioClient
import sentinel.alertservice.server.AlertServiceConfig

/**
  * Provides a new instance of MinioClient
  *
  * @param config endpoint, accesskey, secretkey
  */
class MinioClientProvider @Inject()(config: AlertServiceConfig) extends Provider[MinioClient] {
  override def get(): MinioClient = new MinioClient(config.minioEndpoint, config.minioAccessKey, config.minioSecretKey)
}
