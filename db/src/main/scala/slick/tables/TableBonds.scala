package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableBonds.Bond

class TableBonds(tag: Tag) extends Table[Bond](tag, "bond") {
  def bondsMapId: Rep[Long]  = column[Long]("bonds_map_id")
  def validatorId: Rep[Long] = column[Long]("validator_id")
  def stake: Rep[Long]       = column[Long]("stake")

  def pk  = primaryKey("pk_bond", (bondsMapId, validatorId))
  def fk1 = foreignKey("fk_bond_bonds_map_id", bondsMapId, slick.qBondsMaps)(_.id)
  def fk2 = foreignKey("fk_bond_validator_id", validatorId, slick.qValidators)(_.id)

  def * : ProvenShape[Bond] = (bondsMapId, validatorId, stake).mapTo[Bond]
}

object TableBonds {
  final case class Bond(
    bondsMapId: Long,  // pointer to a bonds map
    validatorId: Long, // pointer to a validator
    stake: Long,       // stake of a validator
  )
}
