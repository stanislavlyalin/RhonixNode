package db

import cats.effect.Sync
import cats.syntax.all.*
import sdk.CustomTypeMode.*
import sdk.RhonixNodeDb.validatorTable
import sdk.db.DbSession.withSessionF
import sdk.db.{DbSession, Validator, ValidatorDbApi}

class ValidatorDbApiImpl[F[_]: Sync: DbSession] extends ValidatorDbApi[F] {
  override def insert(validator: Validator): F[Long] =
    withSessionF(validatorTable.insert(Validator.toDb(0L, validator))).map(_.id)

  override def update(id: Long, validator: Validator): F[Unit] =
    withSessionF(validatorTable.update(Validator.toDb(id, validator)))

  override def getById(id: Long): F[Option[Validator]] =
    withSessionF(validatorTable.where(_.id === id).headOption.map(v => Validator.fromDb(v)))

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] =
    withSessionF(validatorTable.where(_.publicKey === publicKey).headOption.map(v => Validator.fromDb(v)))
}

object ValidatorDbApiImpl {
  def apply[F[_]: ValidatorDbApiImpl]: ValidatorDbApiImpl[F] = implicitly[ValidatorDbApiImpl[F]]
}
