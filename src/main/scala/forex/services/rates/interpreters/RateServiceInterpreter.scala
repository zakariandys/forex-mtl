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

  /**
   * Retrieves the exchange rate for the given currency pair from the cache.
   *
   * @param pair The currency pair to look up.
   * @return An effect `F` that produces either a `Right` with the `Rate` or a `Left` with a `ServiceError`.
   */
  override def get(pair: Rate.Pair): F[Either[errors.ServiceError, Rate]] = {
    val cacheKey = s"${pair.from}${pair.to}"
    cache.get(cacheKey) match {
      case Some(cachedRateJson) => processCachedRate(cachedRateJson)
      case None                 => Sync[F].pure(Left(errors.ServiceError.RateNotFoundError("Rate not found in cache.")))
    }
  }

  private def processCachedRate(cachedRateJson: String): F[Either[errors.ServiceError, Rate]] = {
    decode[Rate](cachedRateJson) match {
      case Right(cachedRate) => validateCachedRate(cachedRate)
      case Left(_)           => Sync[F].pure(Left(errors.ServiceError.RateParseError("Failed to parse cached rate.")))
    }
  }

  private def validateCachedRate(cachedRate: Rate): F[Either[errors.ServiceError, Rate]] = {
    val offsetDateTime = cachedRate.timestamp.value.atZoneSameInstant(ZoneOffset.UTC)
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    if (Duration.between(offsetDateTime, now).toMinutes <= config.timeToLeaveInMinutes) {
      Sync[F].pure(Right(cachedRate))
    } else {
      Sync[F].pure(Left(errors.ServiceError.RateOutdatedError("Cached rate is outdated.")))
    }
  }
}
