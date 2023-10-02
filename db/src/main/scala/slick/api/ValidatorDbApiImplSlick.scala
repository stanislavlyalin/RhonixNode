package slick.api

import cats.effect.Async
import sdk.api.ValidatorDbApi
import sdk.api.data.Validator
import slick.jdbc.JdbcProfile
import slick.{SlickDb, SlickQuery}

class ValidatorDbApiImplSlick[F[_]: Async: SlickDb] extends ValidatorDbApi[F] {
  implicit val p: JdbcProfile = SlickDb[F].profile

  override def insert(publicKey: Array[Byte]): F[Long] =
    SlickDb[F].run(SlickQuery().insertValidator(publicKey))

  override def update(validator: Validator): F[Int] =
    SlickDb[F].run(SlickQuery().updateValidator(validator))

  override def getById(id: Long): F[Option[Validator]] =
    SlickDb[F].run(SlickQuery().getValidatorById(id))

  override def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]] =
    SlickDb[F].run(SlickQuery().getValidatorByPublicKey(publicKey))
}
