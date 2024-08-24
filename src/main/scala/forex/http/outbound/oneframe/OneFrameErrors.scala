package forex.http.outbound.oneframe

object OneFrameErrors {
  sealed trait OneFrameError extends Exception

  object OneFrameError {
    final case class DecodeParsingError(msg: String) extends OneFrameError
    final case class BadRequest(msg: String) extends OneFrameError
    final case class InvalidCurrencyPair(msg: String) extends OneFrameError
    final case class InternalServerError(msg: String) extends OneFrameError
    final case class UnknownSystemError(msg: String) extends OneFrameError
  }
}
