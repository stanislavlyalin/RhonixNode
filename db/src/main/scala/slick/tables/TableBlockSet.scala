package slick.tables

import slick.data.BlockSet
import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class TableBlockSet(tag: Tag) extends Table[BlockSet](tag, "block_set") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash", O.Unique)

  def * : ProvenShape[BlockSet] = (id, hash).mapTo[BlockSet]
}
