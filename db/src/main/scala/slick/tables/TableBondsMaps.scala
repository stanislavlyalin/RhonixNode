package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableBondsMaps.BondsMap

class TableBondsMaps(tag: Tag) extends Table[BondsMap](tag, "bonds_map") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash")

  def idx                       = index("idx_bonds_map", hash, unique = true)
  def * : ProvenShape[BondsMap] = (id, hash).mapTo[BondsMap]
}

object TableBondsMaps {
  final case class BondsMap(
    id: Long,         // primary key
    hash: Array[Byte],// strong hash of a bonds map content (global identifier)
  )
}
