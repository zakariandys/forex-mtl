package forex

import cats.effect.{ConcurrentEffect, Timer}
import forex.config.ApplicationConfig
import forex.http.inbound.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.rates.interpreters.LiveRateServiceInterpreter
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}
import org.http4s.{HttpApp, HttpRoutes}
import fs2.Stream

class Module[F[_]: ConcurrentEffect: Timer](config: ApplicationConfig, httpClient: Client[F]) {

  private val ratesCacheService: RatesMemCacheService[F, String, String] = RatesMemCacheService.ratesMemCacheInterpreter[F, String, String]

  private val ratesService: RatesService[F] = RatesServices.ratesServiceInterpreter(ratesCacheService, config.cacheConfig)

  private val oneFrameService: OneFrameService[F] = OneFrameService.oneFrameInterpreter(httpClient, config.oneFrameClientConfig)

  private val liveRatesService: LiveRateServiceInterpreter[F] = LiveRatesService.live(oneFrameService, ratesCacheService, config.schedulerConfig)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

  val liveRatesServiceStream: Stream[F, Unit] = liveRatesService.stream

}
