package slick.api

import cats.effect.Async
import cats.syntax.all.*
import sdk.api.ValidatorDbApi
import sdk.api.data.Validator
import slick.jdbc.JdbcBackend.DatabaseDef
import slick.jdbc.PostgresProfile.api.*
import slick.tables.ValidatorTableSlick.validatorTableSlick
import slick.tx

import scala.concurrent.ExecutionContext.Implicits.global

class ValidatorDbApiImplSlick[F[_]: Async](dbDef: DatabaseDef) extends ValidatorDbApi[F] {
  override def insert(validator: Validator): F[Long] =
    tx(
      dbDef.run(
        (validatorTableSlick.map(r => (r.publicKey, r.http)) returning validatorTableSlick.map(
          _.id,
        )) += (validator.publicKey, validator.http),
      ),
    )

  override def update(id: Long, validator: Validator): F[Unit] =
    tx(
      dbDef.run(
        validatorTableSlick
          .filter(_.id === id)
          .map(record => (record.publicKey, record.http))
          .update((validator.publicKey, validator.http))
          .map(_ => ()),
      ),
    )

  override def getById(id: Long): F[Option[Validator]] =
    tx(dbDef.run(validatorTableSlick.filter(_.id === id).result.headOption)).map(recordOpt =>
      recordOpt.map { case (_, publicKey, http) => Validator(publicKey, http) },
    )

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] =
    tx(dbDef.run(validatorTableSlick.filter(_.publicKey === publicKey).result.headOption))
      .map(recordOpt => recordOpt.map { case (_, publicKey, http) => Validator(publicKey, http) })
}
