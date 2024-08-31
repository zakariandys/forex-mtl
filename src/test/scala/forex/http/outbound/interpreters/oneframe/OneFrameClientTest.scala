package forex.http.outbound.interpreters.oneframe

import cats.effect.{IO, Resource}
import forex.config.OneFrameClientConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.outbound.oneframe.OneFrameErrors
import forex.http.outbound.oneframe.Protocol.RateResponse
import forex.http.outbound.oneframe.interpreters.OneFrameInterpreter
import org.http4s.client.Client
import org.http4s.{Request, Response, Status}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime


class OneFrameClientTest extends AnyFunSuiteLike with Matchers with MockitoSugar {

  val mockHttpClient: Client[IO] = mock[Client[IO]]

  val config = OneFrameClientConfig(
    host = "localhost",
    port = 8080,
    path = "/rates",
    token = "some-token"
  )

  val oneFrameClient = new OneFrameInterpreter[IO](mockHttpClient, config)

  test("OneFrameClient should fetch rates successfully when the API returns a valid response") {
    val mockRateResponse = """[{"from":"USD","to":"EUR","bid":1.2,"ask":1.3,"price":1.25,"time_stamp":"2024-08-05T06:40:37.613Z"}]"""
    val response = Response[IO](status = Status.Ok).withEntity(mockRateResponse)
    val resource = Resource.eval(IO.pure(response))
    when(mockHttpClient.run(any[Request[IO]])).thenReturn(resource)

    val result = oneFrameClient.fetchRates(Seq(Rate.Pair(Currency.USD, Currency.EUR))).unsafeRunSync()

    val expectedRateResponse = List(
      RateResponse(
        Currency.USD,
        Currency.EUR,
        Price(1.2),
        Price(1.3),
        Price(1.25),
        new Timestamp(OffsetDateTime.parse("2024-08-05T06:40:37.613Z")))
    )

    assert(result == Right(expectedRateResponse))
  }

  test("OneFrameClient should return an InternalServerError for unexpected status codes") {
    val response = Response[IO](status = Status.InternalServerError)
    val resource = Resource.eval(IO.pure(response))
    when(mockHttpClient.run(any[Request[IO]])).thenReturn(resource)

    val result = oneFrameClient.fetchRates(Seq(Rate.Pair(Currency.USD, Currency.EUR))).unsafeRunSync()
    assert(result == Left(OneFrameErrors.OneFrameError.InternalServerError("The server encountered an internal error.")))
  }

  test("OneFrameClient should return a ParsingError when the response cannot be parsed") {
    val mockInvalidRateResponse =
     """["from":"USD","to":"EUR","bid":1.2,"ask":1.3,"price":1.25,"time_stamp":"2024-08-05T06:40:37.613Z"}]"""
    val response = Response[IO](status = Status.Ok).withEntity(mockInvalidRateResponse)
    val resource = Resource.eval(IO.pure(response))
    when(mockHttpClient.run(any[Request[IO]])).thenReturn(resource)

    val result = oneFrameClient.fetchRates(Seq(Rate.Pair(Currency.USD, Currency.EUR))).unsafeRunSync()
    assert(result == Left(OneFrameErrors.OneFrameError.DecodeParsingError("Failed to parsing response.")))
  }

  test("OneFrameClient should return an BadRequest for unexpected status codes") {
    val response = Response[IO](status = Status.BadRequest)
    val resource = Resource.eval(IO.pure(response))
    when(mockHttpClient.run(any[Request[IO]])).thenReturn(resource)

    val result = oneFrameClient.fetchRates(Seq(Rate.Pair(Currency.USD, Currency.EUR))).unsafeRunSync()

    assert(result == Left(OneFrameErrors.OneFrameError.BadRequest("Bad request.")))
  }

  test("OneFrameClient should return any other error status code") {
    val response = Response[IO](status = Status.BadGateway)
    val resource = Resource.eval(IO.pure(response))
    when(mockHttpClient.run(any[Request[IO]])).thenReturn(resource)

    val result = oneFrameClient.fetchRates(Seq(Rate.Pair(Currency.USD, Currency.EUR))).unsafeRunSync()

    assert(result == Left(OneFrameErrors.OneFrameError.UnknownSystemError("Unknown system error.")))
  }

}
