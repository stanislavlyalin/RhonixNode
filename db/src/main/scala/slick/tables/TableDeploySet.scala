package slick.tables

import slick.data.DeploySet
import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class TableDeploySet(tag: Tag) extends Table[DeploySet](tag, "deploy_set") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash", O.Unique)

  def * : ProvenShape[DeploySet] = (id, hash).mapTo[DeploySet]
}
