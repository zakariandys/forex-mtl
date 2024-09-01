package forex

import forex.http.outbound.oneframe.Algebra
import forex.services.cache.Cache
import forex.services.rates.{LiveServiceAlgebra, ServiceAlgebra}

package object services {
  type RatesService[F[_]] = ServiceAlgebra[F]
  final val RatesServices = rates.Interpreters

  type LiveRatesService[F[_]] = LiveServiceAlgebra[F]
  final val LiveRatesService = rates.Interpreters

  type OneFrameService[F[_]] = Algebra[F]
  final val OneFrameService = http.outbound.oneframe.Interpreters

  type RatesMemCacheService[F[_], K, V] = Cache[K, V]
  final val RatesMemCacheService = cache.Interpreters
}
