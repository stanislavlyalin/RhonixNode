package slick.tables

import slick.data.BondSetBind
import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class TableBondSetBind(tag: Tag) extends Table[BondSetBind](tag, "bond_set_bind") {
  def bondSetId: Rep[Long] = column[Long]("bond_set_id")
  def bondId: Rep[Long]    = column[Long]("bond_id")

  def pk  = primaryKey("pk_bond_set_bind", (bondSetId, bondId))
  def fk1 = foreignKey("fk_bond_set_bind_bond_set_id", bondSetId, slick.qBondSet)(_.id)
  def fk2 = foreignKey("fk_bond_set_bind_bond_id", bondId, slick.qBond)(_.id)
  def idx = index("idx_bond_set_bind", bondSetId, unique = false)

  def * : ProvenShape[BondSetBind] = (bondSetId, bondId).mapTo[BondSetBind]
}
