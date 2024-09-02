package forex.services.rates.interpreters

import cats.effect.IO
import forex.config.CacheConfig
import forex.domain.{Currency, Price, Rate, Timestamp}
import forex.services.cache.Cache
import forex.services.rates.errors.ServiceError
import io.circe.syntax.EncoderOps
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import io.circe.generic.auto._

import java.time.OffsetDateTime

class RateServiceInterpreterTest extends AnyFunSuiteLike with Matchers with MockitoSugar with BeforeAndAfterEach {

  var mockCache: Cache[String, String] = _
  var config: CacheConfig = _
  var rateService: RateServiceInterpreter[IO] = _

  override def beforeEach(): Unit = {
    mockCache = mock[Cache[String, String]]
    config = CacheConfig(timeToLeaveInMinutes = 10)
  }

  test("get should return the rate from the cache if it is valid and not outdated") {
    // Arrange
    val timestamp = Timestamp(OffsetDateTime.now().minusMinutes(5))
    val mockedRate = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.5), timestamp)
    val mockedRateJson = mockedRate.asJson.noSpaces

    when(mockCache.get("USDEUR")).thenReturn(Some(mockedRateJson))
    rateService = new RateServiceInterpreter[IO](mockCache, config)

    // Act
    val result = rateService.get(Rate.Pair(Currency.USD, Currency.EUR)).unsafeRunSync()

    // Assert
    assert(result == Right(mockedRate))
  }

  test("get should return a RateOutdatedError if the cached rate is outdated") {
    // Arrange
    val outdatedTimestamp = Timestamp(OffsetDateTime.now().minusMinutes(15))
    val outdatedRate = Rate(Rate.Pair(Currency.USD, Currency.EUR), Price(1.5), outdatedTimestamp)
    val outdatedRateJson = outdatedRate.asJson.noSpaces

    when(mockCache.get("USDEUR")).thenReturn(Some(outdatedRateJson))
    rateService = new RateServiceInterpreter[IO](mockCache, config)

    // Act
    val result = rateService.get(Rate.Pair(Currency.USD, Currency.EUR)).unsafeRunSync()

    // Assert
    assert(result == Left(ServiceError.RateOutdatedError("Cached rate is outdated.")))
  }

  test("get should return a RateParseError if the cached rate JSON is invalid") {
    // Arrange
    val invalidJson = """{"invalid":"json"}"""

    when(mockCache.get("USDEUR")).thenReturn(Some(invalidJson))
    rateService = new RateServiceInterpreter[IO](mockCache, config)

    // Act
    val result = rateService.get(Rate.Pair(Currency.USD, Currency.EUR)).unsafeRunSync()

    // Assert
    assert(result == Left(ServiceError.RateParseError("Failed to parse cached rate.")))
  }

  test("get should return a RateNotFoundError if the rate is not found in the cache") {
    // Arrange
    when(mockCache.get("USDEUR")).thenReturn(None)
    rateService = new RateServiceInterpreter[IO](mockCache, config)

    // Act
    val result = rateService.get(Rate.Pair(Currency.USD, Currency.EUR)).unsafeRunSync()

    // Assert
    assert(result == Left(ServiceError.RateNotFoundError("Rate not found in cache.")))
  }


}