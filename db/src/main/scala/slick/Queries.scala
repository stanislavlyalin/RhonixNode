package slick

import slick.jdbc.JdbcProfile
import slick.tables.*

final case class Queries(profile: JdbcProfile) {
  import profile.api.*

  /** Config */

  def configValue(key: String): Query[Rep[String], String, Seq] = qConfigs.filter(_.name === key).map(_.value)

  /** Shard */

  def shards: Query[Rep[String], String, Seq] = qShards.map(_.name)

  private def shardById(shardId: Rep[Long]): Query[TableShards, TableShards.Shard, Seq] =
    qShards.filter(_.id === shardId)

  def shardNameById(shardId: Long): Query[Rep[String], String, Seq] = shardById(shardId).map(_.name)

  def shardIdByName(name: String): Query[Rep[Long], Long, Seq] = qShards.filter(_.name === name).map(_.id)

  def shardForCleanUp(shardId: Long): Query[TableShards, TableShards.Shard, Seq] = {
    val relatedDeploysExist = qDeploys.filter(_.shardId === shardId).exists
    qShards.filter(_.id === shardId && !relatedDeploysExist)
  }

  /** Deployer */

  def deployers: Query[Rep[Array[Byte]], Array[Byte], Seq] = qDeployers.map(_.pubKey)

  def deployerIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qDeployers.filter(_.pubKey === pK).map(_.id)

  def deployerForCleanUp(deployerId: Long): Query[TableDeployers, TableDeployers.Deployer, Seq] = {
    val relatedDeploysExist = qDeploys.filter(_.deployerId === deployerId).exists
    qDeployers.filter(_.id === deployerId && !relatedDeploysExist)
  }

  /** Deploy */

  def deploys: Query[Rep[Array[Byte]], Array[Byte], Seq] = qDeploys.map(_.sig)

  def deployById(id: Long): Query[TableDeploys, TableDeploys.Deploy, Seq] = qDeploys.filter(_.id === id)

  def deployBySig(sig: Array[Byte]): Query[TableDeploys, TableDeploys.Deploy, Seq] =
    qDeploys.filter(_.sig === sig)

  def deployIdBySig(sig: Array[Byte]): Query[Rep[Long], Long, Seq] = deployBySig(sig).map(_.id)

  def deployIdsBySigs(sigs: Seq[Array[Byte]]): Query[Rep[Long], Long, Seq] = qDeploys.filter(_.sig inSet sigs).map(_.id)

  def deployWithDataBySig(
    sig: Array[Byte],
  ): Query[(TableDeploys, Rep[Array[Byte]], Rep[String]), (TableDeploys.Deploy, Array[Byte], String), Seq] = for {
    deploy   <- deployBySig(sig)
    shard    <- qShards if deploy.shardId === shard.id
    deployer <- qDeployers if deploy.deployerId === deployer.id
  } yield (deploy, deployer.pubKey, shard.name)

  def deploySigsByDeploySetHash(hash: Array[Byte]): Query[Rep[Array[Byte]], Array[Byte], Seq] = for {
    dsb <- qDeploySetBinds.filter(_.deploySetId in qDeploySets.filter(_.hash === hash).map(_.id))
    d   <- qDeploys if d.id === dsb.deployId
  } yield d.sig

  def deploySetData(deploySetId: Long): Query[(Rep[Array[Byte]], Rep[Array[Byte]]), (Array[Byte], Array[Byte]), Seq] =
    for {
      ds  <- qDeploySets.filter(_.id === deploySetId)
      dsb <- qDeploySetBinds if dsb.deploySetId === ds.id
      d   <- qDeploys if d.id === dsb.deployId
    } yield (ds.hash, d.sig)

  /** Block */

  def blocks: Query[Rep[Array[Byte]], Array[Byte], Seq] = qBlocks.map(_.hash)

  def blockIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] = blockByHash(hash).map(_.id)

  def blockByHash(hash: Array[Byte]): Query[TableBlocks, TableBlocks.Block, Seq] = qBlocks.filter(_.hash === hash)

  def blockHashesByBlockSetHash(hash: Array[Byte]): Query[Rep[Array[Byte]], Array[Byte], Seq] = for {
    bsb <- qBlockSetBinds.filter(_.blockSetId in qBlockSets.filter(_.hash === hash).map(_.id))
    b   <- qBlocks if b.id === bsb.blockId
  } yield b.hash

  /** DeploySet */

  def deploySets: Query[Rep[Array[Byte]], Array[Byte], Seq] = qDeploySets.map(_.hash)

  def deploySetIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qDeploySets.filter(_.hash === hash).map(_.id)

  /** BlockSet */

  def blockSets: Query[Rep[Array[Byte]], Array[Byte], Seq] = qBlockSets.map(_.hash)

  def blockSetIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] = qBlockSets.filter(_.hash === hash).map(_.id)

  def blockSetData(blockSetId: Long): Query[(Rep[Array[Byte]], Rep[Array[Byte]]), (Array[Byte], Array[Byte]), Seq] =
    for {
      bs  <- qBlockSets.filter(_.id === blockSetId)
      bsb <- qBlockSetBinds if bsb.blockSetId === bs.id
      b   <- qBlocks if b.id === bsb.blockId
    } yield (bs.hash, b.hash)

  /** BondsMap */

  def bondsMap: Query[Rep[Array[Byte]], Array[Byte], Seq] = qBondsMaps.map(_.hash)

  def bondsMapIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] = qBondsMaps.filter(_.hash === hash).map(_.id)

  def bondsMapByHash(hash: Array[Byte]): Query[(Rep[Array[Byte]], Rep[Long]), (Array[Byte], Long), Seq] = for {
    b <- qBonds.filter(_.bondsMapId in qBondsMaps.filter(_.hash === hash).map(_.id))
    v <- qValidators if v.id === b.validatorId
  } yield (v.pubKey, b.stake)

  def bondsMapData(
    bondsMapId: Long,
  ): Query[(Rep[Array[Byte]], (Rep[Array[Byte]], Rep[Long])), (Array[Byte], (Array[Byte], Long)), Seq] = for {
    bm <- qBondsMaps.filter(_.id === bondsMapId)
    b  <- qBonds if b.bondsMapId === bm.id
    v  <- qValidators if v.id === b.validatorId
  } yield (bm.hash, (v.pubKey, b.stake))

  /** Validator */

  def validatorIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qValidators.filter(_.pubKey === pK).map(_.id)

  def validatorPkById(validatorId: Long): Query[Rep[Array[Byte]], Array[Byte], Seq] =
    qValidators.filter(_.id === validatorId).map(_.pubKey)
}
