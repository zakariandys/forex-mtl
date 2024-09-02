package forex.http
package inbound.rates

import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.errors.ProgramError.RateLookupFailed
import forex.programs.rates.{Protocol => RatesProgramProtocol}
import io.circe.syntax.EncoderOps
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap(Sync[F].fromEither).flatMap { rate =>
        Ok(rate.asGetApiResponse)
      }.handleErrorWith {
        case e: RateLookupFailed =>
          BadRequest(ErrorResponse(e.msg).asJson)
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
