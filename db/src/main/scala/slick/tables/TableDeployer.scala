package slick.tables

import slick.data.Deployer
import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class TableDeployer(tag: Tag) extends Table[Deployer](tag, "deployer") {
  def id: Rep[Long]            = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def pubKey: Rep[Array[Byte]] = column[Array[Byte]]("pub_key", O.Unique)

  def idx = index("idx_deployer", pubKey, unique = true)

  def * : ProvenShape[Deployer] = (id, pubKey).mapTo[Deployer]
}
