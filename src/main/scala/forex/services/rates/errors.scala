package forex.services.rates

object errors {

  sealed trait ServiceError
  object ServiceError {
    final case class OneFrameLookupFailed(msg: String) extends ServiceError
    final case class RateOutdatedError(msg: String) extends ServiceError
    final case class RateParseError(msg: String) extends ServiceError
    final case class RateNotFoundError(msg: String) extends ServiceError
  }

}
