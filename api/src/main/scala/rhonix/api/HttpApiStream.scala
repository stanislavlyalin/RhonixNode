package rhonix.api

import cats.effect.ExitCode
import cats.effect.kernel.{Async, Sync}
import cats.effect.std.Console
import cats.syntax.all.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.syntax.all.*
import org.http4s.{EntityEncoder, HttpApp, HttpRoutes}
import sdk.api.FindApi
import sdk.syntax.all.*

import scala.concurrent.duration.DurationInt

object HttpApiStream {
  def httpRoute[F[_]: Sync, T](findApi: FindApi[F, String, T])(implicit ei: EntityEncoder[F, T]): HttpRoutes[F] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "balance" / id =>
      findApi.get(id).flatMap(x => Ok(x))
    }
  }

  def apply[F[_]: Async: Console](
    routes: HttpRoutes[F],
    httpPort: Int = 8080,
    host: String = "localhost",
  ): fs2.Stream[F, ExitCode] =
    BlazeServerBuilder[F] // TODO why NettyServerBuilder doesn't work?
      .bindHttp(httpPort, host)
      .withHttpApp(HttpApp[F](routes.orNotFound.run))
      .withIdleTimeout(1.seconds)
      .serve
}
