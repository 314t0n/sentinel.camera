package sentinel.alertservice.server
import com.typesafe.config.Config

class AlertServiceConfig(config: Config) {

  val storageBucketId: String  = config.getString("storageBucketId")
  val minioEndpoint: String  = config.getString("minio.endpoint")
  val minioAccessKey: String = config.getString("minio.accesskey")
  val minioSecretKey: String = config.getString("minio.secretkey")
  val storageRetryTimes: Int = config.getInt("storage.fail.retry.times")

}
