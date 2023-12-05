package node.api

import cats.effect.{Async, ExitCode}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.{HttpApp, HttpRoutes}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

package object web {
  def server[F[_]: Async](
    routes: HttpRoutes[F],
    httpPort: Int = 8080,
    host: String = "localhost",
    devMode: Boolean = false,
  ): fs2.Stream[F, ExitCode] = {
    // Allow CORS requests when in the dev mode. This allows swagger-ui to serve openApi schema loaded
    // directly from the node regardless of the swagger-ui webpage origin.
    val service = if (devMode) CORS.policy.withAllowOriginAll(routes) else routes

    BlazeServerBuilder[F]
      .bindHttp(httpPort, host)
      .withHttpApp(HttpApp[F](service.orNotFound.run))
      .withIdleTimeout(1.seconds)
      .serve
  }
}
