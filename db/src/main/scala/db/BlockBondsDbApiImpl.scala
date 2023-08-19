package db

import cats.effect.Sync
import cats.syntax.all.*
import sdk.CustomTypeMode.*
import sdk.RhonixNodeDb.blockBondsTable
import sdk.db.DbSession.withSessionF
import sdk.db.{BlockBonds, BlockBondsDbApi, DbSession}

class BlockBondsDbApiImpl[F[_]: Sync: DbSession] extends BlockBondsDbApi[F] {
  override def insert(blockBonds: BlockBonds): F[BlockBonds] =
    withSessionF(blockBondsTable.insert(BlockBonds.toDb(blockBonds))).map(BlockBonds.fromDb)

  override def getByBlock(blockId: Long): F[Seq[BlockBonds]] =
    withSessionF(blockBondsTable.where(_.blockId === blockId).map(BlockBonds.fromDb).toSeq)
}

object BlockBondsDbApiImpl {
  def apply[F[_]: BlockBondsDbApiImpl]: BlockBondsDbApiImpl[F] = implicitly[BlockBondsDbApiImpl[F]]
}
