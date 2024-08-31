package forex.services.rates

import cats.effect.{Concurrent, Sync, Timer}
import forex.config.CacheConfig
import forex.http.outbound.oneframe.Algebra
import forex.services.cache.Cache
import forex.services.rates.interpreters.{LiveRateServiceInterpreter, RateServiceInterpreter}

object Interpreters {
  def ratesServiceInterpreter[F[_]: Sync](cache: Cache[String, String], config: CacheConfig): ServiceAlgebra[F] =
    new RateServiceInterpreter[F](cache, config)

  def live[F[_]: Concurrent: Timer](client: Algebra[F],
                                    cache: Cache[String, String],
                                    config: CacheConfig): LiveRateServiceInterpreter[F] =
    new LiveRateServiceInterpreter[F](client, cache, config)
}
