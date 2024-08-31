package forex.services.rates.interpreters

import cats.effect.Sync
import forex.config.CacheConfig
import forex.domain.Rate
import forex.services.cache.Cache
import forex.services.rates.{ServiceAlgebra, errors}
import io.circe.generic.auto._
import io.circe.parser.decode

import java.time.{Duration, OffsetDateTime, ZoneOffset}

/**
 * RateServiceInterpreter provides the functionality to retrieve currency rates from the cache
 * and handle any errors.
 *
 * @param cache  The cache where currency rates are stored.
 * @param config The cache configuration, including time-to-live settings.
 * @tparam F     The effect type
 */
class RateServiceInterpreter[F[_] : Sync](cache: Cache[String, String], config: CacheConfig)
  extends ServiceAlgebra[F]{

  //todo: refine the implementation
  //todo: enrich error handling
  override def get(pair: Rate.Pair): F[Either[errors.ServiceError, Rate]] = {
    val cacheKey = s"${pair.from}${pair.to}"
    println(cacheKey)
    println(cache.get(cacheKey))

    cache.get(cacheKey) match {
      case Some(cachedRateJson) =>
        decode[Rate](cachedRateJson) match {
          case Right(cachedRate) =>
            val offsetDateTime = cachedRate.timestamp.value.atZoneSameInstant(ZoneOffset.UTC)
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            if (Duration.between(offsetDateTime, now).toMinutes <= config.timeToLeaveInMinutes) {
              Sync[F].pure(Right(cachedRate))
            } else {
              Sync[F].pure(Left(errors.ServiceError.RateOutdatedError("Cached rate is outdated.")))
            }
          case Left(_) =>
            Sync[F].pure(Left(errors.ServiceError.RateParseError("Failed to parse cached rate JSON.")))
        }

      case None =>
        Sync[F].pure(Left(errors.ServiceError.RateNotFoundError("Rate not found in cache.")))
    }
  }
}
