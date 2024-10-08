package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    oneFrameClientConfig: OneFrameClientConfig,
    cacheConfig: CacheConfig,
    schedulerConfig: SchedulerConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class OneFrameClientConfig(
    host: String,
    port: Int,
    path: String,
    token: String
)

case class CacheConfig(
    timeToLeaveInMinutes: Int
)

case class SchedulerConfig(
  intervalInMinutes: Int
)
