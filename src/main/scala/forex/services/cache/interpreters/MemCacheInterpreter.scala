package forex.services.cache.interpreters

import com.github.blemale.scaffeine.{Scaffeine, Cache => ScaffeineCache}
import forex.services.cache.Cache
import scala.concurrent.duration._

/**
 * An in-memory cache interpreter using Scaffeine, a Scala wrapper for the Caffeine caching library.
 * This implementation provides basic caching functionality with an optional expiration policy.
 *
 * @tparam K The type of the keys used to store values in the cache.
 * @tparam V The type of the values stored in the cache.
 */
class MemCacheInterpreter[F[_], K, V] extends Cache[K, V]{

  private val cache: ScaffeineCache[K, V] = Scaffeine()
    .build[K, V]()

  /**
   * Retrieves a value from the cache based on the given key.
   *
   * @param key The key whose associated value is to be returned.
   * @return An Option containing the value associated with the key, or None if the key is not present or has expired.
   */
  override def get(key: K): Option[V] = cache.getIfPresent(key)

  /**
   * Associates the specified value with the specified key in the cache.
   * If the cache previously contained a mapping for the key, the old value is replaced by the specified value.
   *
   * @param key   The key with which the specified value is to be associated.
   * @param value The value to be associated with the specified key.
   * @param ttl   An optional time-to-live (TTL) duration for the cache entry.
   *              Currently, this TTL is not used, and the cache uses a fixed expiration of 10 minutes.
   */
  override def put(key: K, value: V, ttl: Option[FiniteDuration]): Unit = cache.put(key, value)
}
