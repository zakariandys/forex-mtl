package forex.programs.rates

import forex.services.rates.errors.ServiceError

object errors {

  sealed trait ProgramError extends Exception
  object ProgramError {
    final case class RateLookupFailed(msg: String) extends ProgramError
  }

  def toProgramError(error: ServiceError): ProgramError = error match {
    case ServiceError.OneFrameLookupFailed(msg) => ProgramError.RateLookupFailed(msg)
    case ServiceError.RateNotFoundError(msg) => ProgramError.RateLookupFailed(msg) //todo: fix this dummy parses
    case ServiceError.RateOutdatedError(msg) => ProgramError.RateLookupFailed(msg) //todo: fix this dummy parses
    case ServiceError.RateParseError(msg) => ProgramError.RateLookupFailed(msg) //todo: fix this dummy parses
  }
}
