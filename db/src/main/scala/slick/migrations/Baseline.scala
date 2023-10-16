package slick.migrations

import slick.migration.api.{Dialect, ReversibleMigrationSeq, TableMigration}
import slick.*

// Initial version of the database
object Baseline {
  def apply()(implicit dialect: Dialect[?]): ReversibleMigrationSeq = {
    val BlockTM = TableMigration(qBlock).create
      .addColumns(
        _.id,
        _.version,
        _.hash,
        _.sigAlg,
        _.signature,
        _.finalStateHash,
        _.postStateHash,
        _.validatorId,
        _.shardId,
        _.justificationSetId,
        _.seqNum,
        _.offencesSet,
        _.bondsMapId,
        _.finalFringe,
        _.deploySetId,
        _.mergeSetId,
        _.dropSetId,
        _.mergeSetFinalId,
        _.dropSetFinalId,
      )
      .addForeignKeys(_.fk1, _.fk2, _.fk3, _.fk4, _.fk5, _.fk6, _.fk7, _.fk8, _.fk9, _.fk10, _.fk11)
      .addIndexes(_.idx)

    val BlockSetTM = TableMigration(qBlockSet).create
      .addColumns(_.id, _.hash)

    val BlockSetBindTM = TableMigration(slick.qBlockSetBind).create
      .addColumns(_.blockSetId, _.blockId)
      .addPrimaryKeys(_.pk)
      .addForeignKeys(_.fk1, _.fk2)
      .addIndexes(_.idx)

    val bondTM = TableMigration(qBond).create
      .addColumns(_.id, _.validatorId, _.stake)

    val bondSetTM = TableMigration(qBondSet).create
      .addColumns(_.id, _.hash)

    val BondSetBindTM = TableMigration(qBondSetBind).create
      .addColumns(_.bondSetId, _.bondId)
      .addPrimaryKeys(_.pk)
      .addForeignKeys(_.fk1, _.fk2)
      .addIndexes(_.idx)

    val DeployTM = TableMigration(slick.qDeploy).create
      .addColumns(_.id, _.sig, _.deployerId, _.shardId, _.program, _.phloPrice, _.phloLimit, _.nonce)
      .addForeignKeys(_.fk1, _.fk2)
      .addIndexes(_.idx)

    val DeployerTM = TableMigration(slick.qDeployer).create
      .addColumns(_.id, _.pubKey)
      .addIndexes(_.idx)

    val DeploySetTM = TableMigration(slick.qDeploySet).create
      .addColumns(_.id, _.hash)

    val DeploySetBindTM = TableMigration(slick.qDeploySetBind).create
      .addColumns(_.deploySetId, _.deployId)
      .addPrimaryKeys(_.pk)
      .addForeignKeys(_.fk1, _.fk2)
      .addIndexes(_.idx)

    val ShardTM = TableMigration(qShard).create
      .addColumns(_.id, _.name)

    val validatorTM = TableMigration(qValidator).create
      .addColumns(_.id, _.pubKey)
      .addIndexes(_.idx)

    val configTable = TableMigration(slick.config).create
      .addColumns(_.name, _.value)

    validatorTM & ShardTM & DeployerTM & DeployTM & DeploySetTM & DeploySetBindTM & bondTM & bondSetTM & BondSetBindTM & BlockSetTM & BlockTM & BlockSetBindTM & configTable
  }
}
