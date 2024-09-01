package forex.services.cache

import forex.services.cache.interpreters.MemCacheInterpreter

object Interpreters {
  def ratesMemCacheInterpreter[F[_], K, V]: Cache[K, V] = new MemCacheInterpreter[F, K, V]()
}
