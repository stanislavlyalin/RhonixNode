package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableBlockSets.BlockSet

class TableBlockSets(tag: Tag) extends Table[BlockSet](tag, "block_set") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash", O.Unique)

  def * : ProvenShape[BlockSet] = (id, hash).mapTo[BlockSet]
}

object TableBlockSets {
  final case class BlockSet(
    id: Long,         // primary key
    hash: Array[Byte],// strong hash of a hashes of blocks in a set (global identifier)
  )
}
