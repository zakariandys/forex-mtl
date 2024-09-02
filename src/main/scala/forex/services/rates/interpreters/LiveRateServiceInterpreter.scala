package forex.services.rates.interpreters

import cats.effect.{Async, Concurrent, Sync, Timer}
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.flatMap._
import forex.config.SchedulerConfig
import forex.domain.{Currency, Rate}
import forex.http.outbound.oneframe.OneFrameErrors
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
 * @param config  The scheduler configuration, including the fetch interval.
 * @tparam F      The effect type
 */
class LiveRateServiceInterpreter[F[_]: Concurrent: Timer](client: OneFrameService[F],
                                                          cache: Cache[String, String],
                                                          config: SchedulerConfig) extends LiveServiceAlgebra[F] {

  private final val TIME_OF_INTERVAL: FiniteDuration = config.intervalInMinutes.minutes


  /**
   * This stream is used for setting up a scheduled task that runs at a fixed interval.
   *
   * @return A `Stream[F, Unit]`
   */
  def stream: Stream[F, Unit] = {
    Stream.awakeEvery[F](TIME_OF_INTERVAL)
      .evalMap(_ => fetchAndProcessRates())
  }

  /**
   * Fetches currency rates from the OneFrameService, processes them, and stores them in the cache.
   *
   * @return F[Unit]
   */
  override def fetchAndProcessRates(): F[Unit] = {
    fetchRatesFromClient().flatMap {
      case Right(rates: List[RateResponse]) =>
        Sync[F].fromEither(putCache(rates))
          .handleErrorWith { e =>
            handleError(OneFrameErrors.OneFrameError.InternalServerError(s"Failed put to cache: ${e.getMessage}"))
        }
      case Left(error: OneFrameErrors.OneFrameError) =>
        handleError(error)
    }
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

  private def fetchRatesFromClient(): F[Either[OneFrameErrors.OneFrameError, List[RateResponse]]] = {
    client.fetchRates(Currency.pairOfCurrencies)
  }

  private def putCache(rates: List[RateResponse]): Either[OneFrameErrors.OneFrameError, Unit] = {
    try {
      rates.foreach { rate =>
        val cacheKey = s"${rate.from}${rate.to}"
        val cacheValue = RateConverter.fromRateResponse(rate)
        cache.put(cacheKey, cacheValue.asJson.noSpaces, None)
      }
      Right(())
    } catch {
      case ex: Exception => Left(OneFrameErrors.OneFrameError.UnknownSystemError(s"Failed to put rates into cache: ${ex.getMessage}"))
    }
  }

  private def handleError(error: OneFrameErrors.OneFrameError): F[Unit] = {
    Async[F].pure {
      println(s"An error occurred: ${error.getMessage}") // todo: use logger instead
    }
  }

}