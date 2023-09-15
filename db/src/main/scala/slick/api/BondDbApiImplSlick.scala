package slick.api

import cats.effect.Async
import sdk.api.BondDbApi
import sdk.api.data.Bond
import slick.jdbc.JdbcBackend.DatabaseDef

class BondDbApiImplSlick[F[_]: Async](dbDef: DatabaseDef) extends BondDbApi[F] {
  override def insert(bond: Bond, validatorId: Long): F[Long] = ???

  override def update(id: Long, bond: Bond, validatorId: Long): F[Unit] = ???

  override def getById(id: Long): F[Option[Bond]] = ???
}
