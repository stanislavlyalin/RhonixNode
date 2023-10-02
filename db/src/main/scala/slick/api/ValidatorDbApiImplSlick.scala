package slick.api

import cats.effect.Async
import cats.syntax.all.*
import sdk.api.ValidatorDbApi
import sdk.api.data.Validator
import slick.jdbc.JdbcProfile
import slick.{SlickDb, SlickQuery}

class ValidatorDbApiImplSlick[F[_]: Async](db: SlickDb) extends ValidatorDbApi[F] {
  implicit val p: JdbcProfile = db.profile

  override def insert(validator: Validator): F[Long] =
    db.run(SlickQuery().insertValidator(validator))

  override def update(id: Long, validator: Validator): F[Unit] =
    db.run(SlickQuery().updateValidator(id, validator)).void

  override def getById(id: Long): F[Option[Validator]] =
    db.run(SlickQuery().getValidatorById(id))

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] =
    db.run(SlickQuery().getValidatorByPublicKey(publicKey))
}
