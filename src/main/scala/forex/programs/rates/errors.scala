package forex.programs.rates

import forex.services.rates.errors.ServiceError

object errors {

  sealed trait ProgramError extends Exception
  object ProgramError {
    final case class RateLookupFailed(msg: String) extends ProgramError
    final case class RateOutdatedError(msg: String) extends ProgramError
    final case class RateParseError(msg: String) extends ProgramError
  }

  def toProgramError(error: ServiceError): ProgramError = error match {
    case ServiceError.RateNotFoundError(msg) => ProgramError.RateLookupFailed(msg)
    case ServiceError.RateOutdatedError(msg) => ProgramError.RateOutdatedError(msg)
    case ServiceError.RateParseError(msg)    => ProgramError.RateParseError(msg)
  }
}
