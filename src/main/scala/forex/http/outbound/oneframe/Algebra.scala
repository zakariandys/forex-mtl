package forex.http.outbound.oneframe

import forex.domain.Rate
import forex.http.outbound.oneframe.Protocol.RateResponse

trait Algebra[F[_]] {
  def fetchRates(pairs: Seq[Rate.Pair]) : F[Either[OneFrameErrors.OneFrameError, List[RateResponse]]]
}
