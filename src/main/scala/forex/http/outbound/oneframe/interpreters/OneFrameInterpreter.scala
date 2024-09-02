package forex.http.outbound.oneframe.interpreters

import cats.effect.{Async}
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.flatMap._
import forex.config.OneFrameClientConfig
import forex.domain.Rate
import forex.http.outbound.oneframe.OneFrameErrors.OneFrameError._
import forex.http.outbound.oneframe.Protocol.{ErrorRateResponse, RateResponse}
import forex.http.outbound.oneframe.{Algebra, OneFrameErrors}
import org.http4s.Method.GET
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.typelevel.ci.CIString


class OneFrameInterpreter[F[_]: Async](httpClient: Client[F],
                                       config: OneFrameClientConfig)
  extends Algebra[F] {

  /**
   * Fetches exchange rates for the given currency pairs from the OneFrame API.
   *
   * @param pairs The sequence of currency pairs for which the rates are to be fetched.
   * @return A result wrapped in the effect type `F`, which contains either:
   *         - `Right(List[RateResponse])` if the rates were successfully fetched and parsed, or
   *         - `Left(OneFrameErrors.OneFrameError)` if an error occurred, such as a bad request,
   *           server error, or parsing failure.
   */
  override def fetchRates(pairs: Seq[Rate.Pair]): F[Either[OneFrameErrors.OneFrameError, List[RateResponse]]] = {
    val uri: Uri = composeUri(pairs)
    val httpRequest = Request[F](method = GET, uri = uri)
      .withHeaders(Header.Raw(CIString("token"), config.token))

    httpClient.run(httpRequest).use { response =>
      handleResponse(response)
    }
  }

  private def composeUri(pairs: Seq[Rate.Pair]): Uri = {
    val queryParams = pairs.map(pair => "pair" -> s"${pair.from}${pair.to}")

    Uri(
      scheme = Some(Uri.Scheme.http),
      authority = Some(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port))),
      path = Uri.Path.unsafeFromString(config.path),
      query = Query.fromPairs(queryParams: _*)
    )
  }

  private def handleErrorRateResponse(response: Response[F]): F[Either[OneFrameErrors.OneFrameError, List[RateResponse]]] = {
    response.as[ErrorRateResponse].attempt.flatMap {
      case Right(errorResponse) if errorResponse.error == "Quota reached" =>
        Async[F].pure(Left(MaxQuotaReachedError("Quota reached.")))
      case Right(errorResponse) if errorResponse.error == "Invalid Currency Pair" =>
        Async[F].pure(Left(InvalidCurrencyPairError("Invalid Currency Pair.")))
      case Right(_) =>
        Async[F].pure(Left(DecodeParsingError(s"Unknown error response")))
      case Left(_) =>
        Async[F].pure(Left(DecodeParsingError("Failed to parsing response.")))
    }
  }

  private def handleOkResponse(response: Response[F]): F[Either[OneFrameErrors.OneFrameError, List[RateResponse]]] = {
    response.as[List[RateResponse]].attempt.flatMap {
      case Right(rates) => Async[F].pure(Right(rates))
      case Left(_)      => handleErrorRateResponse(response)
    }
  }

  private def handleResponse(response: Response[F]): F[Either[OneFrameErrors.OneFrameError, List[RateResponse]]] = {
    response.status match {
      case Status.Ok                  => handleOkResponse(response)
      case Status.BadRequest          => Async[F].pure(Left(BadRequest("Bad request.")))
      case Status.Forbidden           => Async[F].pure(Left(Forbidden("Unable to access resource.")))
      case Status.InternalServerError => Async[F].pure(Left(InternalServerError("The server encountered an internal error.")))
      case _ => Async[F].pure(Left(UnknownSystemError("Unknown system error.")))
    }
  }
}
