package forex.services.rates

trait LiveServiceAlgebra[F[_]]{
  def fetchAndProcessRates(): F[Unit]
}
