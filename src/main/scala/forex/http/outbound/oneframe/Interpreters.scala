package forex.http.outbound.oneframe

import cats.effect.Async
import forex.config.OneFrameClientConfig
import forex.http.outbound.oneframe.interpreters.OneFrameInterpreter
import org.http4s.client.Client

object Interpreters {
  def oneFrameInterpreter[F[_]: Async](httpClient: Client[F], config: OneFrameClientConfig): Algebra[F] =
    new OneFrameInterpreter[F](httpClient, config);
}
