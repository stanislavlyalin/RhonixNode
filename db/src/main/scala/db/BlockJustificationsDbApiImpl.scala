package db

import cats.effect.Sync
import cats.syntax.all.*
import sdk.CustomTypeMode.*
import sdk.RhonixNodeDb.blockJustificationsTable
import sdk.db.DbSession.withSessionF
import sdk.db.{BlockJustifications, BlockJustificationsDbApi, DbSession}

class BlockJustificationsDbApiImpl[F[_]: Sync: DbSession] extends BlockJustificationsDbApi[F] {
  override def insert(blockJustifications: BlockJustifications): F[BlockJustifications] =
    withSessionF(blockJustificationsTable.insert(BlockJustifications.toDb(blockJustifications)))
      .map(BlockJustifications.fromDb)

  override def getByBlock(latestBlockId: Long): F[Seq[BlockJustifications]] =
    withSessionF(
      blockJustificationsTable.where(_.latestBlockId === latestBlockId).map(BlockJustifications.fromDb).toSeq,
    )
}

object BlockJustificationsDbApiImpl {
  def apply[F[_]: BlockJustificationsDbApiImpl]: BlockJustificationsDbApiImpl[F] =
    implicitly[BlockJustificationsDbApiImpl[F]]
}
