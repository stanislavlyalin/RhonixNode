package slick

import slick.jdbc.JdbcProfile
import slick.tables.*

final case class Queries(profile: JdbcProfile) {
  import profile.api.*

  /** Config */

  def configValue(key: String): Query[Rep[String], String, Seq] = qConfigs.filter(_.name === key).map(_.value)

  /** Shard */

  def shards: Query[Rep[String], String, Seq] = qShards.map(_.name)

  def shardById(shardId: Rep[Long]): Query[TableShards, TableShards.Shard, Seq] = qShards.filter(_.id === shardId)

  def shardNameById(shardId: Long): Query[Rep[String], String, Seq] = shardById(shardId).map(_.name)

  def shardIdByName(name: String): Query[Rep[Long], Long, Seq] = qShards.filter(_.name === name).map(_.id)

  def shardForCleanUp(shardId: Long): Query[TableShards, TableShards.Shard, Seq] = {
    val relatedDeploysExist = qDeploys.filter(_.shardId === shardId).exists
    qShards.filter(_.id === shardId && !relatedDeploysExist)
  }

  /** Deployer */

  def deployers: Query[Rep[Array[Byte]], Array[Byte], Seq] = qDeployers.map(_.pubKey)

  def deployerById(id: Rep[Long]): Query[TableDeployers, TableDeployers.Deployer, Seq] = qDeployers.filter(_.id === id)

  def deployerIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qDeployers.filter(_.pubKey === pK).map(_.id)

  def deployerForCleanUp(deployerId: Long): Query[TableDeployers, TableDeployers.Deployer, Seq] = {
    val relatedDeploysExist = qDeploys.filter(_.deployerId === deployerId).exists
    qDeployers.filter(_.id === deployerId && !relatedDeploysExist)
  }

  /** Deploy */

  def deploys: Query[Rep[Array[Byte]], Array[Byte], Seq] = qDeploys.map(_.sig)

  def deployById(id: Long): Query[TableDeploys, TableDeploys.Deploy, Seq] = qDeploys.filter(_.id === id)

  def deployBySig(sig: Array[Byte]): Query[TableDeploys, TableDeploys.Deploy, Seq] = qDeploys.filter(_.sig === sig)

  def deployIdBySig(sig: Array[Byte]): Query[Rep[Long], Long, Seq] = deployBySig(sig).map(_.id)

  def deployIdsBySigs(sigs: Seq[Array[Byte]]): Query[Rep[Long], Long, Seq] = qDeploys.filter(_.sig inSet sigs).map(_.id)

  def deploySigsByIds(ids: Seq[Long]): Query[Rep[Array[Byte]], Array[Byte], Seq] =
    qDeploys.filter(_.id inSet ids).map(_.sig)

  def deployIdDeployerShardBySig(sig: Array[Byte]): Query[(Rep[Long], Rep[Long], Rep[Long]), (Long, Long, Long), Seq] =
    deployBySig(sig).map(d => (d.id, d.deployerId, d.shardId))

  /** Block */

  def blocks: Query[Rep[Array[Byte]], Array[Byte], Seq] = qBlocks.map(_.hash)

  def blockIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] = blockByHash(hash).map(_.id)

  def blockByHash(hash: Array[Byte]): Query[TableBlocks, TableBlocks.Block, Seq] = qBlocks.filter(_.hash === hash)

  def blockHashesByIds(ids: Seq[Long]): Query[Rep[Array[Byte]], Array[Byte], Seq] =
    qBlocks.filter(_.id inSet ids).map(_.hash)

  /** DeploySet */

  def deploySets: Query[Rep[Array[Byte]], Array[Byte], Seq] = qDeploySets.map(_.hash)

  def deploySetIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qDeploySets.filter(_.hash === hash).map(_.id)

  def deploySetHashById(id: Long): Query[Rep[Array[Byte]], Array[Byte], Seq] =
    qDeploySets.filter(_.id === id).map(_.hash)

  def deployIdsByDeploySetId(deploySetId: Long): Query[Rep[Long], Long, Seq] =
    qDeploySetBinds.filter(_.deploySetId === deploySetId).map(_.deployId)

  /** BlockSet */

  def blockSets: Query[Rep[Array[Byte]], Array[Byte], Seq] = qBlockSets.map(_.hash)

  def blockSetIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] = qBlockSets.filter(_.hash === hash).map(_.id)

  def blockSetHashById(id: Long): Query[Rep[Array[Byte]], Array[Byte], Seq] = qBlockSets.filter(_.id === id).map(_.hash)

  def blockIdsByBlockSetId(blockSetId: Long): Query[Rep[Long], Long, Seq] =
    qBlockSetBinds.filter(_.blockSetId === blockSetId).map(_.blockId)

  /** BondsMap */

  def bondsMap: Query[Rep[Array[Byte]], Array[Byte], Seq] = qBondsMaps.map(_.hash)

  def bondsMapIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] = qBondsMaps.filter(_.hash === hash).map(_.id)

  def bondsMapHashById(id: Long): Query[Rep[Array[Byte]], Array[Byte], Seq] = qBondsMaps.filter(_.id === id).map(_.hash)

  def bondIdsByBondsMapId(bondsMapId: Long): Query[(Rep[Long], Rep[Long]), (Long, Long), Seq] =
    qBonds.filter(_.bondsMapId === bondsMapId).map(b => (b.validatorId, b.stake))

  /** Validator */

  def validatorIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qValidators.filter(_.pubKey === pK).map(_.id)

  def validatorPkById(validatorId: Long): Query[Rep[Array[Byte]], Array[Byte], Seq] =
    qValidators.filter(_.id === validatorId).map(_.pubKey)
}
