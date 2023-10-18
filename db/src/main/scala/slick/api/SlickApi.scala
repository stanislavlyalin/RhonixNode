package slick.api

import cats.effect.Async
import slick.jdbc.JdbcProfile
import slick.syntax.all.*
import slick.tables.TableValidators.Validator
import slick.{SlickDb, SlickQuery}

class SlickApi[F[_]: Async: SlickDb] {
  implicit val p: JdbcProfile = SlickDb[F].profile
  val queries: SlickQuery     = SlickQuery()

  def validatorInsert(pubKey: Array[Byte]): F[Long] = queries.validatorInsert(pubKey).run

  def validatorUpdate(validator: Validator): F[Int] = queries.validatorUpdate(validator).run

  def validatorGetById(id: Long): F[Option[Validator]] = queries.validatorGetById(id).run

  def validatorGetByPK(pubKey: Array[Byte]): F[Option[Validator]] = queries.validatorGetByPK(pubKey).run
}
