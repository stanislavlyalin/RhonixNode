package slick.api

import cats.effect.Async
import cats.implicits.toFunctorOps
import sdk.primitive.ByteArray
import slick.jdbc.JdbcProfile
import slick.syntax.all.*
import slick.tables.TableValidators.Validator
import slick.{SlickDb, SlickQuery}

import scala.concurrent.ExecutionContext

class SlickApi[F[_]: Async: SlickDb] {
  implicit val p: JdbcProfile       = SlickDb[F].profile
  implicit val ec: ExecutionContext = ExecutionContext.global
  val queries: SlickQuery           = SlickQuery()

  def validatorInsert(pubKey: Array[Byte]): F[Long] = queries.validatorInsert(pubKey).run

  def validatorUpdate(validator: Validator): F[Int] = queries.validatorUpdate(validator).run

  def validatorGetById(id: Long): F[Option[Validator]] = queries.validatorGetById(id).run

  def validatorGetByPK(pubKey: Array[Byte]): F[Option[Validator]] = queries.validatorGetByPK(pubKey).run

  def shardGetAll: F[Seq[String]] = queries.shardGetAll.run

  def shardDelete(name: String): F[Int] = queries.shardDelete(name).run

  def deployerGetAll: F[Seq[ByteArray]] = queries.deployerGetAll.run.map(_.map(ByteArray(_)))

  def deployerDelete(pK: ByteArray): F[Int] = queries.deployerDelete(pK.bytes).run

  def deployInsert(d: sdk.data.Deploy): F[Unit] = queries
    .deployInsertIfNot(d.sig.bytes, d.deployerPk.bytes, d.shardName, d.program, d.phloPrice, d.phloLimit, d.nonce)
    .run
    .void

  def deployGetAll: F[Seq[ByteArray]] = queries.deployGetAll.run.map(_.map(ByteArray(_)))

  def deployGet(sig: ByteArray): F[Option[sdk.data.Deploy]] = queries
    .deployGetData(sig.bytes)
    .run
    .map(
      _.map { case (deploy, deployerPK, shardName) =>
        sdk.data.Deploy(
          sig = ByteArray(deploy.sig),
          deployerPk = ByteArray(deployerPK),
          shardName = shardName,
          program = deploy.program,
          phloPrice = deploy.phloPrice,
          phloLimit = deploy.phloLimit,
          nonce = deploy.nonce,
        )
      },
    )

  def deployDelete(sig: ByteArray): F[Int] = queries.deployDeleteAndCleanUp(sig.bytes).run
}
