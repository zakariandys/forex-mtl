package forex.http.outbound.oneframe

import forex.domain.{Currency, Price, Timestamp}
import io.circe.{Decoder, HCursor}
import io.circe.generic.extras.Configuration


object Protocol {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
  final case class RateResponse(
                                 from: Currency,
                                 to: Currency,
                                 bid: Price,
                                 ask: Price,
                                 price: Price,
                                 timestamp: Timestamp
                               )

  implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeString.emap { str =>
    Currency.fromString(str) match {
      case currency: Currency => Right(currency)
      case _ => Left(s"Invalid currency code: $str")
    }
  }

  implicit val priceDecoder: Decoder[Price] = Decoder.decodeBigDecimal.map(Price.apply)

  implicit val timestampDecoder: Decoder[Timestamp] = Decoder.decodeOffsetDateTime.map(Timestamp(_))

  implicit val rateResponseDecoder: Decoder[RateResponse] = (c: HCursor) =>
    for {
      from <- c.downField("from").as[Currency]
      to <- c.downField("to").as[Currency]
      bid <- c.downField("bid").as[Price]
      ask <- c.downField("ask").as[Price]
      price <- c.downField("price").as[Price]
      timestamp <- c.downField("time_stamp").as[Timestamp]
    } yield RateResponse(from, to, bid, ask, price, timestamp)

  implicit val rateResponseListDecoder: Decoder[List[RateResponse]] = Decoder.decodeList[RateResponse]
}
