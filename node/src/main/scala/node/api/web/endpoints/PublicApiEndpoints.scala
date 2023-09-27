package node.api.web.endpoints

import cats.syntax.all.*
import endpoints4s.algebra
import node.api.web.json.JsonSchemasPretty
import sdk.api.Path

trait PublicApiEndpoints extends algebra.Endpoints with algebra.JsonEntitiesFromSchemas with JsonSchemasPretty {

  val apiPath: Path[Unit] = Path.foldLeft(path)(_ / _)

  lazy val deployId: Path[String]  = segment[String](name = "id", docs = "id of a deploy".some)
  lazy val blockId: Path[String]   = segment[String](name = "id", docs = "id of a block".some)
  lazy val stateHash: Path[String] = segment[String](name = "hash", docs = "hash of a state".some)
  lazy val walletId: Path[String]  = segment[String](name = "id", docs = "wallet id".some)

  def block[A](implicit r: ResponseEntity[A]): Endpoint[String, A] = endpoint(
    get(apiPath / "block" / blockId),
    ok(r),
    docs = EndpointDocs().withDescription("Get block ny id".some),
  )

  def deploy[A](implicit r: ResponseEntity[A]): Endpoint[String, A] = endpoint(
    get(apiPath / "deploy" / deployId),
    ok(r),
    docs = EndpointDocs().withDescription("Get deploy by id".some),
  )

  def latest[A](implicit r: ResponseEntity[A]): Endpoint[Unit, A] = endpoint(
    get(apiPath / "latest"),
    ok(r),
    docs = EndpointDocs().withDescription("Latest messages".some),
  )

  def balance[A](implicit r: ResponseEntity[A]): Endpoint[(String, String), A] = endpoint(
    get(apiPath / "balance" / stateHash / walletId),
    ok(r),
    docs = EndpointDocs().withDescription("Balance of a wallet at the state specified".some),
  )
}
