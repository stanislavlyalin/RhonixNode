package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class BondTableSlick(tag: Tag) extends Table[(Long, Long, Long)](tag, "Bond") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def validatorId: Rep[Long] = column[Long]("validatorId", O.Unique)
  def stake: Rep[Long]       = column[Long]("stake")

  def * : ProvenShape[(Long, Long, Long)] = (id, validatorId, stake)
}

object BondTableSlick {
  val bondTableSlick = TableQuery[BondTableSlick]
}
