package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableBondSets.BondSet

class TableBondSets(tag: Tag) extends Table[BondSet](tag, "bond_set") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash", O.Unique)

  def * : ProvenShape[BondSet] = (id, hash).mapTo[BondSet]
}

object TableBondSets {
  final case class BondSet(
    id: Long,         // primary key
    hash: Array[Byte],// strong hash of a bonds set content (global identifier)
  )
}
