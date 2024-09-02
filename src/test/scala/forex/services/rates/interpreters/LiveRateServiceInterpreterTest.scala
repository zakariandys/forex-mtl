package forex.services.rates.interpreters

import cats.effect.{Concurrent, ContextShift, IO, Timer}
import forex.config.SchedulerConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.http.outbound.oneframe.OneFrameErrors
import forex.http.outbound.oneframe.Protocol.RateResponse
import forex.services.OneFrameService
import forex.services.cache.Cache
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration




class LiveRateServiceInterpreterTest[F[_]] extends AnyFunSuiteLike with Matchers with MockitoSugar with BeforeAndAfterEach {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val concurrent: Concurrent[IO] = IO.ioConcurrentEffect

  var mockOneFrameService: OneFrameService[IO] = _
  var mockCache: Cache[String, String] = _
  val zeroMinuteSchedulerConfig = SchedulerConfig(intervalInMinutes = 0)

  override def beforeEach(): Unit = {
    mockOneFrameService = mock[OneFrameService[IO]]
    mockCache = mock[Cache[String, String]]
  }

  test("fetchAndProcessRates should successfully fetch rates and store them in the cache") {
    // Arrange
    val mockedRateResponse = RateResponse(Currency.USD, Currency.EUR, Price(1.2), Price(1.3), Price(1.25), Timestamp.now)
    val mockedRateResponses = List(mockedRateResponse)

    // Act
    when(mockOneFrameService.fetchRates(any[Seq[Rate.Pair]])).thenReturn(IO.pure(Right(mockedRateResponses)))
    val liveRateService = new LiveRateServiceInterpreter[IO](mockOneFrameService, mockCache, zeroMinuteSchedulerConfig)
    liveRateService.fetchAndProcessRates().unsafeRunSync()

    // Verify: Ensure put to cache is performed
    val expectedCacheKey = s"${mockedRateResponse.from}${mockedRateResponse.to}"
    val expectedCacheValue = Rate(
      Rate.Pair(mockedRateResponse.from, mockedRateResponse.to),
      mockedRateResponse.price,
      mockedRateResponse.timestamp)
      .asJson.noSpaces

    verify(mockCache).put(
      expectedCacheKey,
      expectedCacheValue,
      None
    )
  }

  test("stream should fetch and process rates at regular intervals") {
    // Arrange
    val mockedRateResponse = RateResponse(Currency.USD, Currency.EUR, Price(1.2), Price(1.3), Price(1.25), Timestamp.now)
    val mockedRateResponses = List(mockedRateResponse)

    // Act
    when(mockOneFrameService.fetchRates(any[Seq[Rate.Pair]])).thenReturn(IO.pure(Right(mockedRateResponses)))
    val liveRateService = new LiveRateServiceInterpreter[IO](mockOneFrameService, mockCache, zeroMinuteSchedulerConfig)
    liveRateService.stream.take(1).compile.drain.unsafeRunSync()

    // Verify
    verify(mockOneFrameService).fetchRates(any[Seq[Rate.Pair]])

    val expectedCacheKey = s"${mockedRateResponse.from}${mockedRateResponse.to}"
    val expectedCacheValue = Rate(
      Rate.Pair(mockedRateResponse.from, mockedRateResponse.to),
      mockedRateResponse.price,
      mockedRateResponse.timestamp)
      .asJson.noSpaces

    verify(mockCache).put(
      expectedCacheKey,
      expectedCacheValue,
      None
    )
  }

  test("RateConverter.fromRateResponse should correctly convert RateResponse to Rate") {
    // Arrange
    val mockedRateResponse = RateResponse(Currency.USD, Currency.EUR, Price(1.2), Price(1.3), Price(1.25), Timestamp.now)

    // Act
    val liveRateService = new LiveRateServiceInterpreter[IO](mockOneFrameService, mockCache, zeroMinuteSchedulerConfig)
    val rate = liveRateService.RateConverter.fromRateResponse(mockedRateResponse)

    // Assert: Verify that the conversion was done correctly
    assert(rate.pair.from == Currency.USD)
    assert(rate.pair.to == Currency.EUR)
    assert(rate.price == mockedRateResponse.price)
    assert(rate.timestamp == mockedRateResponse.timestamp)
  }

  test("fetchAndProcessRates should never performed put to cache") {
    // Arrange
    val clientError = OneFrameErrors.OneFrameError.InternalServerError("Client error")

    // Act
    when(mockOneFrameService.fetchRates(any[Seq[Rate.Pair]])).thenReturn(IO.pure(Left(clientError)))
    val liveRateService = new LiveRateServiceInterpreter[IO](mockOneFrameService, mockCache, zeroMinuteSchedulerConfig)
    liveRateService.fetchAndProcessRates().unsafeRunSync()

    // Assert: Verify that cache will never performed
    verify(mockCache, never).put(any[String], any[String], any[Option[FiniteDuration]])
  }

  test("putCache should return an error if an exception occurs during caching") {
    // Arrange
    val mockedRateResponse = RateResponse(Currency.USD, Currency.EUR, Price(1.2), Price(1.3), Price(1.25), Timestamp.now)
    val mockedRateResponses = List(mockedRateResponse)

    // Act
    when(mockOneFrameService.fetchRates(any[Seq[Rate.Pair]])).thenReturn(IO.pure(Right(mockedRateResponses)))
    when(mockCache.put(any[String], any[String], any[Option[FiniteDuration]])).thenThrow(new RuntimeException())
    val liveRateService = new LiveRateServiceInterpreter[IO](mockOneFrameService, mockCache, zeroMinuteSchedulerConfig)
    liveRateService.fetchAndProcessRates().unsafeRunSync()

    // Assert: Verify that cache will never performed
    verify(mockCache).put(any[String], any[String], any[Option[FiniteDuration]])
    // todo: verify log
  }

}
