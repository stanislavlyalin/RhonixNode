package slick

import slick.jdbc.JdbcProfile
import slick.tables.*

final case class Queries(profile: JdbcProfile) {
  import profile.api.*

  /** Config */

  def configValue(key: String): Query[Rep[String], String, Seq] = qConfigs.filter(_.name === key).map(_.value)

  /** Shard */

  private def shardById(shardId: Rep[Long]): Query[TableShards, TableShards.Shard, Seq] =
    qShards.filter(_.id === shardId)

  def shardNameById(shardId: Long): Query[Rep[String], String, Seq] = shardById(shardId).map(_.name)

  def shardIdByName(name: String): Query[Rep[Long], Long, Seq] = qShards.filter(_.name === name).map(_.id)

  /** Deployer */

  def deployerIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qDeployers.filter(_.pubKey === pK).map(_.id)

  /** Deploy */

  def deploys: Query[Rep[Array[Byte]], Array[Byte], Seq] = qDeploys.map(_.sig)

  private def deployBySig(sig: Array[Byte]): Query[TableDeploys, TableDeploys.Deploy, Seq] =
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

  /** DeploySet */

  def deploySetIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] =
    qDeploySets.filter(_.hash === hash).map(_.id)

  /** BlockSet */

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
