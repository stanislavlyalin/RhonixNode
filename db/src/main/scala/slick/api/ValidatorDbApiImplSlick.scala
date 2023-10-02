package slick.api

import cats.effect.Async
import sdk.api.ValidatorDbApi
import sdk.api.data.Validator
import slick.jdbc.JdbcProfile
import slick.{SlickDb, SlickQuery}

class ValidatorDbApiImplSlick[F[_]: Async](db: SlickDb) extends ValidatorDbApi[F] {
  implicit val p: JdbcProfile = db.profile

  override def insert(publicKey: Array[Byte]): F[Long] =
    db.run(SlickQuery().insertValidator(publicKey))

  override def update(validator: Validator): F[Int] =
    db.run(SlickQuery().updateValidator(validator))

  override def getById(id: Long): F[Option[Validator]] =
    db.run(SlickQuery().getValidatorById(id))

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] =
    db.run(SlickQuery().getValidatorByPublicKey(publicKey))
}
