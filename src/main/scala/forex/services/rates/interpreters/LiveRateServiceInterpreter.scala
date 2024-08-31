package forex.services.rates.interpreters

import cats.effect.{Concurrent, Timer}
import cats.implicits.toFunctorOps
import forex.config.CacheConfig
import forex.domain.{Currency, Rate}
import forex.http.outbound.oneframe.Protocol.RateResponse
import forex.services.OneFrameService
import forex.services.cache.Cache
import forex.services.rates.LiveServiceAlgebra
import fs2.Stream
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * LiveRateServiceInterpreter is an implementation of the LiveServiceAlgebra.
 * It is responsible for fetching currency rates at regular intervals, processing them,
 * and storing them in a cache.
 *
 * @param client  The OneFrameService used to fetch rates.
 * @param cache   The cache used to store fetched rates.
 * @param config  The cache configuration, including the fetch interval.
 * @tparam F      The effect type
 */
class LiveRateServiceInterpreter[F[_]: Concurrent: Timer](client: OneFrameService[F],
                                                          cache: Cache[String, String],
                                                          config: CacheConfig) extends LiveServiceAlgebra[F] {

  private val fetchInterval: FiniteDuration = config.timeToLeaveInMinutes.seconds

  def stream: Stream[F, Unit] = {
    Stream.awakeEvery[F](fetchInterval)
      .evalMap(_ => fetchAndProcessRates())
  }

  object RateConverter {
    def fromRateResponse(rateResponse: RateResponse): Rate = {
      Rate(
        pair = Rate.Pair(rateResponse.from, rateResponse.to),
        price = rateResponse.price,
        timestamp = rateResponse.timestamp
      )
    }
  }


  /**
   * Fetches currency rates from the OneFrameService, processes them, and stores them in the cache.
   *
   * @return F[Unit]
   */
  override def fetchAndProcessRates(): F[Unit] = {
    // todo: use combination generator or use configurable pairs
    val pairs: Seq[Rate.Pair] = Seq(
      Rate.Pair(Currency.USD, Currency.EUR),
      Rate.Pair(Currency.USD, Currency.JPY),
      Rate.Pair(Currency.EUR, Currency.GBP),
      Rate.Pair(Currency.AUD, Currency.CAD),
      Rate.Pair(Currency.CHF, Currency.NZD)
    )

    //todo: refine the implementation, and use log for debug
    client.fetchRates(pairs).map {
      case Right(rates) =>
        rates.foreach{rate =>
          val cacheKey = s"${rate.from}${rate.to}"
          val rateObj = RateConverter.fromRateResponse(rate)
          cache.put(cacheKey, rateObj.asJson.noSpaces, null)

          cache.get(cacheKey)
          println(cache.get(cacheKey))
        }

        println(s"Fetched rates: $rates")
      case Left(error) =>
        println(s"Error fetching rates: $error")
    }
  }

}