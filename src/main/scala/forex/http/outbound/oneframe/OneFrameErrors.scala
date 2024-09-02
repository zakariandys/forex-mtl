package forex.http.outbound.oneframe

object OneFrameErrors {
  sealed trait OneFrameError extends Exception {
    def message: String
    override def getMessage: String = message
  }

  object OneFrameError {
    final case class DecodeParsingError(message: String) extends OneFrameError
    final case class BadRequest(message: String) extends OneFrameError
    final case class Forbidden(message: String) extends OneFrameError
    final case class InvalidCurrencyPairError(message: String) extends OneFrameError
    final case class MaxQuotaReachedError(message: String) extends OneFrameError
    final case class InternalServerError(message: String) extends OneFrameError
    final case class UnknownSystemError(message: String) extends OneFrameError
  }
}
