package sdk.db

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BlockDeploysTable(blockId: Long, deployId: Long)

final case class BlockDeploys(blockId: Long, deployId: Long)

trait BlockDeploysDbApi[F[_]] {
  def insert(blockDeploys: BlockDeploys): F[BlockDeploys]
  def getByBlock(blockId: Long): F[Seq[BlockDeploys]]
}

object BlockDeploys {
  def toDb(blockDeploys: BlockDeploys): BlockDeploysTable =
    BlockDeploysTable(blockDeploys.blockId, blockDeploys.deployId)

  def fromDb(blockDeploys: BlockDeploysTable): BlockDeploys =
    BlockDeploys(blockDeploys.blockId, blockDeploys.deployId)
}
