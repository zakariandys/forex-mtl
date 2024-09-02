package forex.http.inbound.rates

import forex.domain.Currency
import org.http4s.{ParseFailure, QueryParamDecoder}
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap(stringCurrency =>
      Currency.fromString(stringCurrency) match {
        case Some(currency: Currency) => Right(currency)
        case None                     => Left(ParseFailure(s"Invalid currency: $stringCurrency", "Currency not recognized"))
      })

  object FromQueryParam extends QueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends QueryParamDecoderMatcher[Currency]("to")

}
