package node.api.web

import cats.effect.kernel.Concurrent
import endpoints4s.http4s.server
import endpoints4s.http4s.server.Endpoints
import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}
import node.api.web.endpoints.PublicApiEndpoints

import scala.Function.const

/**
 * OpenApi schema definition.
 * */
private object OpenApiJsonPublic
    extends PublicApiEndpoints
    with openapi.Endpoints
    with openapi.JsonEntitiesFromSchemas {

  private val endpoints: Seq[DocumentedEndpoint] = Seq(getBlock, getDeploy, getStatus, getBalance, getLatest)

  val openApi: OpenApi = openApi(Info(title = sdk.api.Title, version = sdk.api.Version))(endpoints*)
}

/**
 * Routes exposing OpenApi documentation.
 * */
final case class DocsJsonRoutes[F[_]: Concurrent]()
    extends Endpoints[F]
    with server.JsonEntitiesFromEncodersAndDecoders {
  implicit val jCodec: endpoints4s.Encoder[OpenApi, String] = OpenApi.stringEncoder

  val public = endpoint(get(path / sdk.api.DocFileName), ok(jsonResponse[OpenApi]))
    .implementedBy(const(OpenApiJsonPublic.openApi))
}
