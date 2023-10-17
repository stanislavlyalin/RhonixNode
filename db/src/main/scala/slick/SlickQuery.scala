package slick

import slick.dbio.Effect.{Read, Transactional, Write}
import slick.jdbc.JdbcProfile
import slick.sql.SqlAction
import slick.tables.TableDeploys
import slick.tables.TableValidators.Validator

import scala.concurrent.ExecutionContext.Implicits.global

final case class SlickQuery()(implicit val profile: JdbcProfile) {
  import Helpers.*
  import profile.api.*

  def validatorGetById(id: Long): SqlAction[Option[Validator], NoStream, Read] =
    qValidators.filter(_.id === id).result.headOption

  def validatorGetByPK(publicKey: Array[Byte]): SqlAction[Option[Validator], NoStream, Read] =
    qValidators.filter(_.pubKey === publicKey).result.headOption

  def validatorInsert(publicKey: Array[Byte]): SqlAction[Long, NoStream, Write] =
    (qValidators.map(r => r.pubKey) returning qValidators.map(_.id)) += publicKey

  def validatorUpdate(validator: Validator): SqlAction[Int, NoStream, Write] =
    qValidators.update(validator)

  def storeValue(key: String, value: String): SqlAction[Int, NoStream, Effect.Write] =
    qConfigs.insertOrUpdate((key, value))

  def loadValue(key: String): SqlAction[Option[String], NoStream, Effect.Read] =
    qConfigs.filter(_.name === key).map(_.value).result.headOption

  /** Get shard id by unique shard name */
  private def shardGetId(name: String): SqlAction[Option[Long], NoStream, Read] =
    qShards.filter(_.name === name).map(_.id).result.headOption

  /** Get a list of all shard names */
  def shardGetAll: SqlAction[Seq[String], NoStream, Read] =
    qShards.map(_.name).result

  /** Insert a new record in table. Returned id. */
  private def shardInsert(name: String): SqlAction[Long, NoStream, Write] =
    (qShards.map(_.name) returning qShards.map(_.id)) += name

  /** Insert a new record in table if there is no such entry. Returned id. */
  private def shardInsertIfNot(name: String): DBIOAction[Long, NoStream, Read & Write & Transactional] =
    insertIfNot(name, shardGetId, name, shardInsert)

  /** Delete entry by unique shard name.
   * Returned 1 if record was deleted. */
  def shardDelete(name: String): SqlAction[Int, NoStream, Write] =
    qShards.filter(_.name === name).delete

  /** Get deployer id by unique public key */
  private def deployerGetId(pK: Array[Byte]): SqlAction[Option[Long], NoStream, Read] =
    qDeployers.filter(_.pubKey === pK).map(_.id).result.headOption

  /** Get a list of all deployer public keys */
  def deployerGetAll: SqlAction[Seq[Array[Byte]], NoStream, Read] =
    qDeployers.map(_.pubKey).result

  /** Insert a new record in table. Returned id. */
  private def deployerInsert(pK: Array[Byte]): SqlAction[Long, NoStream, Write] =
    (qDeployers.map(_.pubKey) returning qDeployers.map(_.id)) += pK

  /** Insert a new record in table if there is no such entry. Returned id */
  private def deployerInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, Read & Write & Transactional] =
    insertIfNot(pK, deployerGetId, pK, deployerInsert)

  /** Delete entry by unique public key. Returned 1 if record was deleted, otherwise 0. */
  def deployerDelete(pK: Array[Byte]): SqlAction[Int, NoStream, Write] =
    qDeployers.filter(_.pubKey === pK).delete

  /** Get deploy id by unique sig */
  private def deployGetId(sig: Array[Byte]): SqlAction[Option[Long], NoStream, Read] =
    qDeploys.filter(_.sig === sig).map(_.id).result.headOption

  /** Get a list of all deploy sigs */
  def deployGetAll: SqlAction[Seq[Array[Byte]], NoStream, Read] =
    qDeploys.map(_.sig).result

  /** Get deploy by unique sig. Returner (TableDeploys.Deploy, shard.name, deployer.pubKey)*/
  def deployGet(sig: Array[Byte]): SqlAction[Option[(TableDeploys.Deploy, Array[Byte], String)], NoStream, Read] = {
    val query = for {
      deploy   <- qDeploys if deploy.sig === sig
      shard    <- qShards if shard.id === deploy.shardId
      deployer <- qDeployers if deployer.id === deploy.deployerId
    } yield (deploy, deployer.pubKey, shard.name)
    query.result.headOption
  }

  private def deployInsert(deploy: TableDeploys.Deploy): SqlAction[Long, NoStream, Write] =
    (qDeploys returning qDeploys.map(_.id)) += deploy

  def deployInsertIfNot(
    sig: Array[Byte],        // deploy signature
    deployerPk: Array[Byte], // deployer public key
    shardName: String,       // unique name of a shard
    program: String,         // code of the program
    phloPrice: Long,         // price offered for phlogiston
    phloLimit: Long,         // limit offered for execution
    nonce: Long,             // nonce of a deploy
  ): DBIOAction[Long, NoStream, Read & Write & Transactional] = {
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
      deployId   <- insertIfNot(sig, deployGetId, newDeploy, deployInsert)
    } yield deployId
    actions.transactionally
  }

  object Helpers {
    def insertIfNot[A, B](
      unique: A,
      getIdByUnique: A => SqlAction[Option[Long], NoStream, Read],
      insertable: B,
      insert: B => SqlAction[Long, NoStream, Write],
    ): DBIOAction[Long, NoStream, Read & Write & Transactional] = {
      val actions = for {
        idOpt <- getIdByUnique(unique)
        id    <- idOpt match {
                   case Some(existingId) => DBIO.successful(existingId)
                   case None             => insert(insertable)
                 }
      } yield id
      actions.transactionally
    }
  }
}
