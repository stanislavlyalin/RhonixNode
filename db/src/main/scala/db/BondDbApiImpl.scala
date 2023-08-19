package db

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all.*
import sdk.CustomTypeMode.*
import sdk.RhonixNodeDb.{bondTable, validatorTable}
import sdk.db.DbSession.withSessionF
import sdk.db.{Bond, BondDbApi, DbSession}

class BondDbApiImpl[F[_]: Sync: DbSession] extends BondDbApi[F] {
  override def insert(bond: Bond, validatorId: Long): F[Long] =
    withSessionF(bondTable.insert(Bond.toDb(0L, bond, validatorId))).map(_.id)

  override def update(id: Long, bond: Bond, validatorId: Long): F[Unit] =
    withSessionF(bondTable.update(Bond.toDb(id, bond, validatorId)))

  override def getById(id: Long): F[Option[Bond]] =
    (for {
      bond      <- OptionT(withSessionF(bondTable.where(_.id === id).headOption))
      validator <- OptionT(withSessionF(validatorTable.where(_.id === bond.validatorId).headOption))
    } yield Bond.fromDb(bond, validator)).value
}

object BondDbApiImpl {
  def apply[F[_]: BondDbApiImpl]: BondDbApiImpl[F] = implicitly[BondDbApiImpl[F]]
}
