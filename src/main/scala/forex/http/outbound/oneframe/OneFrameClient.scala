package forex.http.outbound.oneframe

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import forex.config.OneFrameClientConfig
import forex.domain.Rate
import forex.http.outbound.oneframe.OneFrameErrors.OneFrameError.{BadRequest, DecodeParsingError, InternalServerError, UnknownSystemError}
import forex.http.outbound.oneframe.Protocol.RateResponse
import org.http4s.Method.GET
import org.http4s.{Header, Query, Request, Status, Uri}
import org.http4s.client.Client
import org.typelevel.ci.CIString
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

class OneFrameClient[F[_]: Async](httpClient: Client[F],
                                  config: OneFrameClientConfig)
  extends Algebra[F] {

  /**
   * Composes a URI for fetching exchange rates based on the provided currency pairs.
   *
   * This method constructs a URI that includes the scheme, host, port, path, and query parameters
   * based on the provided sequence of currency pairs. The query parameters are formatted as "pair=FROMTO"
   * where FROM and TO are the currency codes for the pair.
   *
   * @param pairs The sequence of currency pairs for which the rates URI is to be composed.
   * @return A `Uri` object representing the constructed URI.
   */
  def composeUri(pairs: Seq[Rate.Pair]): Uri = {
    val queryParams = pairs.map(pair => "pair" -> s"${pair.from}${pair.to}")

    Uri(
      scheme    = Some(Uri.Scheme.http),
      authority = Some(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port))),
      path      = Uri.Path.unsafeFromString(config.path),
      query     = Query.fromPairs(queryParams: _*)
    )
  }

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
      .withHeaders(Header.Raw(CIString("token"), config.token)) //TODO: Get token from token service provider

    httpClient.run(httpRequest).use { response =>
      response.status match {
        case Status.Ok =>
          response.as[List[RateResponse]].attempt.map {
            case Right(rates) => Right(rates)
            case Left(_) => Left(DecodeParsingError("Failed to parsing response."))
          }
        case Status.BadRequest =>
          Async[F].pure(Left(BadRequest("Bad request.")))
        case Status.InternalServerError =>
          Async[F].pure(Left(InternalServerError("The server encountered an internal error.")))
        case _ =>
          Async[F].pure(Left(UnknownSystemError("Unknown system error.")))
      }
    }
  }
}
