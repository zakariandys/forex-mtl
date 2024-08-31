package forex.services.cache

import scala.concurrent.duration.FiniteDuration

trait Cache[K, V] {

  def get(key: K): Option[V]

  def put(key: K, value: V, ttl: Option[FiniteDuration]): Unit

}
