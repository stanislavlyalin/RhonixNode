package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableDeploys.Deploy

class TableDeploys(tag: Tag) extends Table[Deploy](tag, "deploy") {
  def id: Rep[Long]         = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def sig: Rep[Array[Byte]] = column[Array[Byte]]("sig")
  def deployerId: Rep[Long] = column[Long]("deployer_id")
  def shardId: Rep[Long]    = column[Long]("shard_id")
  def program: Rep[String]  = column[String]("program")
  def phloPrice: Rep[Long]  = column[Long]("phlo_price")
  def phloLimit: Rep[Long]  = column[Long]("phlo_limit")
  def nonce: Rep[Long]      = column[Long]("nonce")

  def fk1 = foreignKey("fk_deploy_deployer_id", deployerId, slick.qDeployers)(_.id)
  def fk2 = foreignKey("fk_deploy_shard_id", shardId, slick.qShards)(_.id)

  def idxSig        = index("idx_deploy_sig", sig, unique = true)
  def idxDeployerId = index("idx_deploy_deployer_id", deployerId, unique = false)
  def idxShardId    = index("idx_deploy_shard_id", shardId, unique = false)

  def * : ProvenShape[Deploy] = (id, sig, deployerId, shardId, program, phloPrice, phloLimit, nonce).mapTo[Deploy]
}

object TableDeploys {
  final case class Deploy(
    id: Long,         // primary key
    sig: Array[Byte], // deploy signature
    deployerId: Long, // pointer to a deployer
    shardId: Long,    // pointer to a shard
    program: String,  // code of the program
    phloPrice: Long,  // price offered for phlogiston
    phloLimit: Long,  // limit offered for execution
    nonce: Long,      // nonce of a deploy
  )
}
