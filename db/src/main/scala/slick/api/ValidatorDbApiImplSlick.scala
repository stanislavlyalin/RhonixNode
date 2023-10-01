package slick.api

import cats.effect.Async
import cats.syntax.all.*
import sdk.api.ValidatorDbApi
import sdk.api.data.Validator
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.syntax.all.*
import slick.validators

class ValidatorDbApiImplSlick[F[_]: Async](db: Database, profile: JdbcProfile) extends ValidatorDbApi[F] {
  import profile.api.*
  override def insert(validator: Validator): F[Long] = {
    val q = (validators.map(r => r.publicKey) returning validators.map(_.id)) += validator.publicKey
    db.runF(q)
  }

  override def update(id: Long, validator: Validator): F[Unit] = {
    val q = validators.filter(_.id === id).map(record => record.publicKey).update(validator.publicKey)
    db.runF(q).void
  }

  override def getById(id: Long): F[Option[Validator]] = {
    val q = validators.filter(_.id === id).result.headOption
    db.runF(q)
  }

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] = {
    val q = validators.filter(_.publicKey === publicKey).result.headOption
    db.runF(q)
  }
}
