package db

import cats.effect.Sync
import cats.syntax.all.*
import sdk.CustomTypeMode.*
import sdk.RhonixNodeDb.blockDeploysTable
import sdk.db.DbSession.withSessionF
import sdk.db.{BlockDeploys, BlockDeploysDbApi, DbSession}

class BlockDeploysDbApiImpl[F[_]: Sync: DbSession] extends BlockDeploysDbApi[F] {
  override def insert(blockDeploys: BlockDeploys): F[BlockDeploys] =
    withSessionF(blockDeploysTable.insert(BlockDeploys.toDb(blockDeploys))).map(BlockDeploys.fromDb)

  override def getByBlock(blockId: Long): F[Seq[BlockDeploys]] = withSessionF(
    blockDeploysTable.where(_.blockId === blockId).map(BlockDeploys.fromDb).toSeq,
  )
}

object BlockDeploysDbApiImpl {
  def apply[F[_]: BlockDeploysDbApiImpl]: BlockDeploysDbApiImpl[F] = implicitly[BlockDeploysDbApiImpl[F]]
}
