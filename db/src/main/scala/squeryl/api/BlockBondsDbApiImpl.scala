package squeryl.api

import cats.Applicative
import cats.syntax.all.*
import sdk.api.BlockBondsDbApi
import sdk.api.data.BlockBonds
import sdk.db.SqlConn
import squeryl.NodeDb.blockBondsTable
import squeryl.tables.BlockBondsTable
import squeryl.CustomTypeMode.*
import squeryl.withSession

class BlockBondsDbApiImpl[F[_]: Applicative: SqlConn] extends BlockBondsDbApi[F] {
  override def insert(blockBonds: BlockBonds): F[BlockBonds] =
    withSession(blockBondsTable.insert(BlockBondsTable.toDb(blockBonds))).map(BlockBondsTable.fromDb)

  override def getByBlock(blockId: Long): F[Seq[BlockBonds]] =
    withSession(blockBondsTable.where(_.blockId === blockId).map(BlockBondsTable.fromDb).toSeq)
}

object BlockBondsDbApiImpl {
  def apply[F[_]: BlockBondsDbApiImpl]: BlockBondsDbApiImpl[F] = implicitly[BlockBondsDbApiImpl[F]]
}
