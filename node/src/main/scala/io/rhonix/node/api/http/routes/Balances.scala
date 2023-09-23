package io.rhonix.node.api.http.routes

import cats.effect.Sync
import cats.syntax.all.*
import io.rhonix.node.api.http.ApiPath
import org.http4s.{EntityEncoder, HttpRoutes}
import sdk.api.BalancesApi

import scala.util.Try

object Balances {
  def apply[F[_]: Sync, A, B, C](
    getApi: (A, B) => F[Option[C]],
  )(implicit decA: String => Try[A], decB: String => Try[B], eeC: EntityEncoder[F, C]): HttpRoutes[F] = {
    val dsl = org.http4s.dsl.Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] { case GET -> ApiPath / BalancesApi.MethodName / a / b =>
      (decA(a), decB(b)).bisequence
        .map { case (a, b) => getApi(a, b).flatMap(_.map(Ok(_)).getOrElse(NotFound())) }
        .getOrElse(BadRequest())
    }
  }
}
