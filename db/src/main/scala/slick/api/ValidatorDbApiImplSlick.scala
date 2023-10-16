package slick.api

import cats.effect.Async
import slick.data.Validator
import slick.jdbc.JdbcProfile
import slick.syntax.all.*
import slick.{SlickDb, SlickQuery}

class ValidatorDbApiImplSlick[F[_]: Async: SlickDb] {
  implicit val p: JdbcProfile = SlickDb[F].profile
  val queries: SlickQuery     = SlickQuery()

  def insert(pubKey: Array[Byte]): F[Long] = queries.insertValidator(pubKey).run

  def update(validator: Validator): F[Int] = queries.updateValidator(validator).run

  def getById(id: Long): F[Option[Validator]] = queries.getValidatorById(id).run

  def getByPublicKey(pubKey: Array[Byte]): F[Option[Validator]] = queries.getValidatorByPubKey(pubKey).run
}
