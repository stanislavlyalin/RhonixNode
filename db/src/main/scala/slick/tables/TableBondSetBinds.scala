package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableBondSetBinds.BondSetBind

class TableBondSetBinds(tag: Tag) extends Table[BondSetBind](tag, "bond_set_bind") {
  def bondSetId: Rep[Long] = column[Long]("bond_set_id")
  def bondId: Rep[Long]    = column[Long]("bond_id")

  def pk  = primaryKey("pk_bond_set_bind", (bondSetId, bondId))
  def fk1 = foreignKey("fk_bond_set_bind_bond_set_id", bondSetId, slick.qBondSets)(_.id)
  def fk2 = foreignKey("fk_bond_set_bind_bond_id", bondId, slick.qBonds)(_.id)
  def idx = index("idx_bond_set_bind", bondSetId, unique = false)

  def * : ProvenShape[BondSetBind] = (bondSetId, bondId).mapTo[BondSetBind]
}

object TableBondSetBinds {
  final case class BondSetBind(
    bondSetId: Long, // pointer to bondSet
    bondId: Long,    // pointer to bond
  )
}
