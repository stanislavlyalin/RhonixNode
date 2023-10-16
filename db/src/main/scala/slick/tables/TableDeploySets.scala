package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableDeploySets.DeploySet

class TableDeploySets(tag: Tag) extends Table[DeploySet](tag, "deploy_set") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash", O.Unique)

  def * : ProvenShape[DeploySet] = (id, hash).mapTo[DeploySet]
}

object TableDeploySets {
  final case class DeploySet(
    id: Long,         // primary key
    hash: Array[Byte],// strong hash of a signatures of deploys in a set (global identifier)
  )
}
