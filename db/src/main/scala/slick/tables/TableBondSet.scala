package slick.tables

import slick.data.BondSet
import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class TableBondSet(tag: Tag) extends Table[BondSet](tag, "bond_set") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash", O.Unique)

  def * : ProvenShape[BondSet] = (id, hash).mapTo[BondSet]
}
