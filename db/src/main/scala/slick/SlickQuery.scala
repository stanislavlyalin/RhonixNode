package slick

import slick.dbio.Effect.{Read, Transactional, Write}
import slick.jdbc.JdbcProfile
import slick.sql.SqlAction
import slick.tables.*
import slick.tables.TableValidators.Validator

import scala.concurrent.ExecutionContext

final case class SlickQuery()(implicit val profile: JdbcProfile, implicit val ec: ExecutionContext) {
  import profile.api.*

  def validatorGetById(id: Long): SqlAction[Option[Validator], NoStream, Read] =
    qValidators.filter(_.id === id).result.headOption

  def validatorGetByPK(publicKey: Array[Byte]): SqlAction[Option[Validator], NoStream, Read] =
    qValidators.filter(_.pubKey === publicKey).result.headOption

  def validatorInsert(publicKey: Array[Byte]): SqlAction[Long, NoStream, Write] =
    (qValidators.map(r => r.pubKey) returning qValidators.map(_.id)) += publicKey

  def storeValue(key: String, value: String): SqlAction[Int, NoStream, Effect.Write] =
    qConfigs.insertOrUpdate((key, value))

  def loadValue(key: String): SqlAction[Option[String], NoStream, Effect.Read] =
    qConfigs.filter(_.name === key).map(_.value).result.headOption

  /** Get a list of all shard names */
  def shardGetAll: SqlAction[Seq[String], NoStream, Read] =
    qShards.map(_.name).result

  /** Get a list of all deployer public keys*/
  def deployerGetAll: SqlAction[Seq[Array[Byte]], NoStream, Read] =
    qDeployers.map(_.pubKey).result

  /** Get a list of all deploy signatures */
  def deployGetAll: SqlAction[Seq[Array[Byte]], NoStream, Read] =
    qDeploys.map(_.sig).result

  /** Get deploy by unique sig. Returned (TableDeploys.Deploy, shard.name, deployer.pubKey)*/
  def deployGetData(sig: Array[Byte]): SqlAction[Option[(TableDeploys.Deploy, Array[Byte], String)], NoStream, Read] = {
    val query = for {
      deploy   <- qDeploys if deploy.sig === sig
      shard    <- qShards if shard.id === deploy.shardId
      deployer <- qDeployers if deployer.id === deploy.deployerId
    } yield (deploy, deployer.pubKey, shard.name)
    query.result.headOption
  }

  /** Insert a new record in table if there is no such entry. Returned id */
  def deployInsertIfNot(
    sig: Array[Byte],        // deploy signature
    deployerPk: Array[Byte], // deployer public key
    shardName: String,       // unique name of a shard
    program: String,         // code of the program
    phloPrice: Long,         // price offered for phlogiston
    phloLimit: Long,         // limit offered for execution
    nonce: Long,             // nonce of a deploy
  ): DBIOAction[Long, NoStream, Effect.All] = {
    def deployerInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, Read & Write & Transactional] = {
      def deployerIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq]      =
        qDeployers.filter(_.pubKey === pK).map(_.id)
      def deployerInsert(pK: Array[Byte]): SqlAction[Long, NoStream, Write] =
        (qDeployers.map(_.pubKey) returning qDeployers.map(_.id)) += pK

      insertIfNot(pK, deployerIdByPK, pK, deployerInsert)
    }
    def shardInsertIfNot(name: String): DBIOAction[Long, NoStream, Read & Write & Transactional]       = {
      def shardIdByName(name: String): Query[Rep[Long], Long, Seq]    =
        qShards.filter(_.name === name).map(_.id)
      def shardInsert(name: String): SqlAction[Long, NoStream, Write] =
        (qShards.map(_.name) returning qShards.map(_.id)) += name

      insertIfNot(name, shardIdByName, name, shardInsert)
    }

    /** Get deploy id by unique signature */
    def deployIdBySig(sig: Array[Byte]): Query[Rep[Long], Long, Seq] =
      qDeploys.filter(_.sig === sig).map(_.id)

    /** Insert a new record in table. Returned id. */
    def deployInsert(deploy: TableDeploys.Deploy): SqlAction[Long, NoStream, Write] =
      (qDeploys returning qDeploys.map(_.id)) += deploy

    val actions = for {
      deployerId <- deployerInsertIfNot(deployerPk)
      shardId    <- shardInsertIfNot(shardName)
      newDeploy   = TableDeploys.Deploy(
                      id = 0L, // Will be replaced by AutoInc
                      sig = sig,
                      deployerId = deployerId,
                      shardId = shardId,
                      program = program,
                      phloPrice = phloPrice,
                      phloLimit = phloLimit,
                      nonce = nonce,
                    )
      deployId   <- insertIfNot(sig, deployIdBySig, newDeploy, deployInsert)
    } yield deployId
    actions.transactionally
  }

  /** Delete deploy by unique sig. And clean up dependencies in Deployers and Shards if possible.
   * Return 1 if deploy deleted, or 0 otherwise. */
  def deployDeleteAndCleanUp(sig: Array[Byte]): DBIOAction[Int, NoStream, Effect.All] = {
    def deployerForCleanUp(deployerId: Long) = {
      val relatedDeploysExist = qDeploys.filter(_.deployerId === deployerId).exists
      qDeployers.filter(d => (d.id === deployerId) && !relatedDeploysExist)
    }

    def shardForCleanUp(shardId: Long) = {
      val relatedDeploysExist = qDeploys.filter(d => d.shardId === shardId).exists
      qShards.filter(s => (s.id === shardId) && !relatedDeploysExist)
    }

    def deployById(id: Long) = qDeploys.filter(_.id === id)

    def deleteAndCleanUp(deployId: Long, deployerId: Long, shardId: Long) = for {
      r <- deployById(deployId).delete
      _ <- deployerForCleanUp(deployerId).delete
      _ <- shardForCleanUp(shardId).delete
    } yield r

    def deployIdsBySig(sig: Array[Byte]) =
      qDeploys.filter(_.sig === sig).map(d => (d.id, d.deployerId, d.shardId))

    val actions = for {
      idsOpt <- deployIdsBySig(sig).result.headOption
      r      <- idsOpt match {
                  case Some((deployId, deployerId, shardId)) => deleteAndCleanUp(deployId, deployerId, shardId)
                  case None                                  => DBIO.successful(0)
                }
    } yield r
    actions.transactionally
  }

  /** Auxiliary function. Insert a new record in table if there is no such entry. Returned id */
  private def insertIfNot[A, B](
    unique: A,
    getIdByUnique: A => Query[Rep[Long], Long, Seq],
    insertable: B,
    insert: B => SqlAction[Long, NoStream, Write],
  ): DBIOAction[Long, NoStream, Read & Write & Transactional] = {
    val actions = for {
      idOpt <- getIdByUnique(unique).result.headOption
      id    <- idOpt match {
                 case Some(existingId) => DBIO.successful(existingId)
                 case None             => insert(insertable)
               }
    } yield id
    actions.transactionally
  }
}
