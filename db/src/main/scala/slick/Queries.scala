package slick

import slick.jdbc.JdbcProfile
import slick.tables.*

final case class Queries(profile: JdbcProfile) {
  import profile.api.*

  /** Config */

  def configValue = Compiled((key: Rep[String]) => qConfigs.filter(_.name === key).map(_.value))

  /** Shard */

  def shardNameById = Compiled((shardId: Rep[Long]) => qShards.filter(_.id === shardId).map(_.name))

  def shardIdByName = Compiled((name: Rep[String]) => qShards.filter(_.name === name).map(_.id))

  /** Deployer */

  def deployerIdByPK = Compiled((pK: Rep[Array[Byte]]) => qDeployers.filter(_.pubKey === pK).map(_.id))

  /** Deploy */

  def deploys = Compiled(qDeploys.map(_.sig))

  private def deployBySig(sig: Rep[Array[Byte]]): Query[TableDeploys, TableDeploys.Deploy, Seq] =
    qDeploys.filter(_.sig === sig)

  def deployIdBySig = Compiled((sig: Rep[Array[Byte]]) => deployBySig(sig).map(_.id))

  def deployIdsBySigs(sigs: Seq[Array[Byte]]) = Compiled(qDeploys.filter(_.sig inSet sigs).map(_.id))

  def deployWithDataBySig = Compiled((sig: Rep[Array[Byte]]) =>
    for {
      deploy   <- deployBySig(sig)
      shard    <- qShards if deploy.shardId === shard.id
      deployer <- qDeployers if deploy.deployerId === deployer.id
    } yield (deploy, deployer.pubKey, shard.name),
  )

  def deploySetData = Compiled((deploySetId: Rep[Long]) =>
    for {
      ds  <- qDeploySets.filter(_.id === deploySetId)
      dsb <- qDeploySetBinds if dsb.deploySetId === ds.id
      d   <- qDeploys if d.id === dsb.deployId
    } yield (ds.hash, d.sig),
  )

  /** Block */

  def blocks = Compiled(qBlocks.map(_.hash))

  def blockIdByHash = Compiled((hash: Rep[Array[Byte]]) => blockByHash(hash).map(_.id))

  def blockByHash(hash: Rep[Array[Byte]]): Query[TableBlocks, TableBlocks.Block, Seq] = qBlocks.filter(_.hash === hash)

  /** DeploySet */

  def deploySetIdByHash = Compiled((hash: Rep[Array[Byte]]) => qDeploySets.filter(_.hash === hash).map(_.id))

  /** BlockSet */

  def blockSetIdByHash = Compiled((hash: Rep[Array[Byte]]) => qBlockSets.filter(_.hash === hash).map(_.id))

  def blockSetData = Compiled((blockSetId: Rep[Long]) =>
    for {
      bs  <- qBlockSets.filter(_.id === blockSetId)
      bsb <- qBlockSetBinds if bsb.blockSetId === bs.id
      b   <- qBlocks if b.id === bsb.blockId
    } yield (bs.hash, b.hash),
  )

  /** BondsMap */

  def bondsMap = Compiled(qBondsMaps.map(_.hash))

  def bondsMapIdByHash = Compiled((hash: Rep[Array[Byte]]) => qBondsMaps.filter(_.hash === hash).map(_.id))

  def bondsMapData = Compiled((bondsMapId: Rep[Long]) =>
    for {
      bm <- qBondsMaps.filter(_.id === bondsMapId)
      b  <- qBonds if b.bondsMapId === bm.id
      v  <- qValidators if v.id === b.validatorId
    } yield (bm.hash, (v.pubKey, b.stake)),
  )

  /** Validator */

  def validatorIdByPK = Compiled((pK: Rep[Array[Byte]]) => qValidators.filter(_.pubKey === pK).map(_.id))

  def validatorPkById = Compiled((validatorId: Rep[Long]) => qValidators.filter(_.id === validatorId).map(_.pubKey))
}
