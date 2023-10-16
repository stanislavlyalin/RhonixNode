package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableDeployers.Deployer

class TableDeployers(tag: Tag) extends Table[Deployer](tag, "deployer") {
  def id: Rep[Long]            = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def pubKey: Rep[Array[Byte]] = column[Array[Byte]]("pub_key", O.Unique)

  def idx = index("idx_deployer", pubKey, unique = true)

  def * : ProvenShape[Deployer] = (id, pubKey).mapTo[Deployer]
}

object TableDeployers {
  final case class Deployer(
    id: Long,           // primary key
    pubKey: Array[Byte],// public key of a deployer
  )
}
