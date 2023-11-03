package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableBlockSetBinds.BlockSetBind

class TableBlockSetBinds(tag: Tag) extends Table[BlockSetBind](tag, "block_set_bind") {
  def blockSetId: Rep[Long] = column[Long]("block_set_id")
  def blockId: Rep[Long]    = column[Long]("block_id")

  def pk = primaryKey("pk_block_set_bind", (blockSetId, blockId))

  def fk1 = foreignKey("fk_block_set_bind_block_set_id", blockSetId, slick.qBlockSets)(_.id)
  def fk2 = foreignKey("fk_block_set_bind_block_id", blockId, slick.qBlocks)(_.id)

  def idx = index("idx_block_set_bind", blockSetId, unique = false)

  def * : ProvenShape[BlockSetBind] = (blockSetId, blockId).mapTo[BlockSetBind]
}

object TableBlockSetBinds {
  final case class BlockSetBind(
    blockSetId: Long, // pointer to blockSet
    blockId: Long,    // pointer to block
  )
}
