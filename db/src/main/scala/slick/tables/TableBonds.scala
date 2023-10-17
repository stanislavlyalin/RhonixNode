package slick.tables

import sdk.api.data.Bond
import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class TableBonds(tag: Tag) extends Table[Bond](tag, "bond") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def validatorId: Rep[Long] = column[Long]("validator_id")
  def stake: Rep[Long]       = column[Long]("stake")

  def idx                   = index("idx_bond", validatorId, unique = true)
  def * : ProvenShape[Bond] = (validatorId, stake).mapTo[Bond]
}

object TableBonds {
  final case class Bond(
    id: Long,          // primary key
    validatorId: Long, // pointer to a validator
    stake: Long,       // stake of a validator
  )
}
