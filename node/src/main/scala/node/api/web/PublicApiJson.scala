package node.api.web

import cats.effect.Concurrent
import cats.syntax.all.*
import endpoints4s.Invalid
import endpoints4s.http4s.server.Endpoints
import node.api.web.endpoints.PublicApiEndpoints
import node.api.web.json.JsonEntitiesCirceFromSchema
import org.http4s.HttpRoutes
import sdk.api.ExternalApi
import sdk.api.data.{Balance, Block, Deploy}
import sdk.codecs.Base16

import scala.Function.const

/** Public JSON API routes. */
final case class PublicApiJson[F[_]: Concurrent](api: ExternalApi[F])
    extends Endpoints[F]
    with PublicApiEndpoints
    with JsonEntitiesCirceFromSchema {

  private def getBalanceByStrings(stateAndWallet: (String, String)): F[Option[Balance]] =
    (Base16.decode(stateAndWallet._1), Base16.decode(stateAndWallet._2)).bisequence.toOption
      .map(api.getBalance.tupled)
      .getOrElse(none[Long].pure[F])
      .map(_.map(new Balance(_)))

  private def getBlockByString: String => F[Option[Block]] =
    Base16.decode(_).toOption.flatTraverse(api.getBlockByHash)

  private def getDeployByString: String => F[Option[Deploy]] =
    Base16.decode(_).toOption.flatTraverse(api.getDeployByHash)

  val routes: HttpRoutes[F] = HttpRoutes.of(
    routesFromEndpoints(
      getBlock.implementedByEffect(getBlockByString),
      getDeploy.implementedByEffect(getDeployByString),
      getBalance.implementedByEffect(getBalanceByStrings),
      getLatest.implementedByEffect(const(api.getLatestMessages)),
      getStatus.implementedByEffect(const(api.status)),
      transferToken.implementedByEffect(
        api.transferToken andThenF
          (_.leftMap(errors => Invalid.apply(errors.map(x => x.show).toList))
            .map(_ => "OK")
            .toEither
            .pure[F]),
      ),
      DocsJsonRoutes().public,
    ),
  )
}
