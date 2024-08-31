package forex.services.cache.interpreters

import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class MemCacheInterpreterTest[F[_]] extends AnyFunSuiteLike with Matchers with MockitoSugar {

  val memCacheInterpreter = new MemCacheInterpreter[F, String, String]

  test("Successfully put rates to cache") {
    memCacheInterpreter.put("USDJPY", "", Some(FiniteDuration.apply(2, TimeUnit.MINUTES)))
  }

  test("Successfully get rates from cache") {
    val mockedResponse = """[{"from":"USD","to":"EUR","bid":1.2,"ask":1.3,"price":1.25,"time_stamp":"2024-08-05T06:40:37.613Z"}]"""
    val mockedCacheData = """[{"from":"USD","to":"EUR","bid":1.2,"ask":1.3,"price":1.25,"time_stamp":"2024-08-05T06:40:37.613Z"}]"""
    memCacheInterpreter.put("USDJPY",
      mockedCacheData,
      Some(FiniteDuration.apply(2, TimeUnit.MINUTES)))

    val result: Option[String] = memCacheInterpreter.get("USDJPY")

    assert(result.get == mockedResponse)
  }
}
