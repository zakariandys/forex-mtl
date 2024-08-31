package forex.services.rates

import forex.domain.Rate
import forex.services.rates.errors._

trait ServiceAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[ServiceError Either Rate]

}
