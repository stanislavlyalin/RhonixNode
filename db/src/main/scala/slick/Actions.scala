package slick

import cats.syntax.all.*
import slick.api.data.{Block, BondsMapData, SetData}
import slick.dbio.Effect.*
import slick.jdbc.JdbcProfile
import slick.tables.*

import scala.concurrent.ExecutionContext

final case class Actions(profile: JdbcProfile, ec: ExecutionContext) {
  private val queries: Queries       = Queries(profile)
  import profile.api.*
  implicit val _ec: ExecutionContext = ec

  /** Config */

  def putConfig(key: String, value: String): DBIOAction[Int, NoStream, Write] = qConfigs.insertOrUpdate((key, value))

  def getConfig(key: String): DBIOAction[Option[String], NoStream, Read] = queries.configValue(key).result.headOption

  /** Shard */

  /** Get a list of all shard names */
  def shardGetAll: DBIOAction[Seq[String], NoStream, Read] = queries.shards.result

  private def shardInsertIfNot(name: String): DBIOAction[Long, NoStream, All] = {
    def shardInsert(name: String): DBIOAction[Long, NoStream, Write] =
      (qShards.map(_.name) returning qShards.map(_.id)) += name

    insertIfNot(name, queries.shardIdByName, name, shardInsert)
  }

  /** Deployer */

  /** Get a list of all deployer public keys */
  def deployerGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] = queries.deployers.result

  private def deployerInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, All] = {
    def deployerInsert(pK: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qDeployers.map(_.pubKey) returning qDeployers.map(_.id)) += pK

    insertIfNot(pK, queries.deployerIdByPK, pK, deployerInsert)
  }

  /** Deploy */

  /** Get a list of all deploy signatures */
  def deployGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] = queries.deploys.result

  /** Get deploy by unique sig. Returned (TableDeploys.Deploy, shard.name, deployer.pubKey) */
  def deployGetData(sig: Array[Byte]): DBIOAction[Option[api.data.Deploy], NoStream, Read] = {
    val query = for {
      deploy   <- queries.deployBySig(sig)
      shard    <- queries.shardById(deploy.shardId)
      deployer <- queries.deployerById(deploy.deployerId)
    } yield (deploy, deployer.pubKey, shard.name)
    query.result.headOption.map(_.map { case (d, pK, sName) =>
      api.data.Deploy(
        sig = d.sig,
        deployerPk = pK,
        shardName = sName,
        program = d.program,
        phloPrice = d.phloPrice,
        phloLimit = d.phloLimit,
        nonce = d.nonce,
      )
    })
  }

  /** Insert a new record in table if there is no such entry. Returned id */
  def deployInsertIfNot(d: api.data.Deploy): DBIOAction[Long, NoStream, All] = {

    /** Insert a new record in table. Returned id. */
    def deployInsert(deploy: TableDeploys.Deploy): DBIOAction[Long, NoStream, Write] =
      (qDeploys returning qDeploys.map(_.id)) += deploy

    val actions = for {
      deployerId <- deployerInsertIfNot(d.deployerPk)
      shardId    <- shardInsertIfNot(d.shardName)
      newDeploy   = TableDeploys.Deploy(
                      id = 0L, // Will be replaced by AutoInc
                      sig = d.sig,
                      deployerId = deployerId,
                      shardId = shardId,
                      program = d.program,
                      phloPrice = d.phloPrice,
                      phloLimit = d.phloLimit,
                      nonce = d.nonce,
                    )
      deployId   <- insertIfNot(d.sig, queries.deployIdBySig, newDeploy, deployInsert)
    } yield deployId

    actions.transactionally
  }

  /** Delete deploy by unique sig. And clean up dependencies in Deployers and Shards if possible.
   * Return 1 if deploy deleted, or 0 otherwise. */
  def deployDeleteAndCleanUp(sig: Array[Byte]): DBIOAction[Int, NoStream, All] = {
    def deleteAndCleanUp(deployId: Long, deployerId: Long, shardId: Long): DBIOAction[Int, NoStream, Write] = for {
      r <- queries.deployById(deployId).delete
      _ <- queries.deployerForCleanUp(deployerId).delete
      _ <- queries.shardForCleanUp(shardId).delete
    } yield r

    queries
      .deployIdDeployerShardBySig(sig)
      .result
      .headOption
      .flatMap {
        case Some((deployId, deployerId, shardId)) => deleteAndCleanUp(deployId, deployerId, shardId)
        case None                                  => DBIO.successful(0)
      }
      .transactionally
  }

  /** Block */

  /** Insert a new Block in table if there is no such entry. Returned id */
  def blockInsertIfNot(b: api.data.Block): DBIOAction[Long, NoStream, All] = {

    def insertBlockSet(setDataOpt: Option[api.data.SetData]): DBIOAction[Option[Long], NoStream, All] =
      setDataOpt match {
        case Some(setData) => blockSetInsertIfNot(setData.hash, setData.data).map(Some(_))
        case None          => DBIO.successful(None)
      }

    def insertDeploySet(setDataOpt: Option[api.data.SetData]): DBIOAction[Option[Long], NoStream, All] =
      setDataOpt match {
        case Some(setData) => deploySetInsertIfNot(setData.hash, setData.data).map(Some(_))
        case None          => DBIO.successful(None)
      }

    def blockInsert(block: TableBlocks.Block): DBIOAction[Long, NoStream, Write] =
      (qBlocks returning qBlocks.map(_.id)) += block

    val actions = for {
      validatorId <- validatorInsertIfNot(b.validatorPk)
      shardId     <- shardInsertIfNot(b.shardName)

      justificationSetId <- insertBlockSet(b.justificationSet)
      offencesSet        <- insertBlockSet(b.offencesSet)
      bondsMapId         <- bondsMapInsertIfNot(b.bondsMap.hash, b.bondsMap.data)
      finalFringe        <- insertBlockSet(b.finalFringe)
      deploySetId        <- insertDeploySet(b.execDeploySet)

      mergeSetId      <- insertDeploySet(b.mergeDeploySet)
      dropSetId       <- insertDeploySet(b.dropDeploySet)
      mergeSetFinalId <- insertDeploySet(b.mergeDeploySetFinal)
      dropSetFinalId  <- insertDeploySet(b.dropDeploySetFinal)

      newBlock = TableBlocks.Block(
                   id = 0L,
                   version = b.version,
                   hash = b.hash,
                   sigAlg = b.sigAlg,
                   signature = b.signature,
                   finalStateHash = b.finalStateHash,
                   postStateHash = b.postStateHash,
                   validatorId = validatorId,
                   shardId = shardId,
                   justificationSetId = justificationSetId,
                   seqNum = b.seqNum,
                   offencesSetId = offencesSet,
                   bondsMapId = bondsMapId,
                   finalFringeId = finalFringe,
                   execDeploySetId = deploySetId,
                   mergeDeploySetId = mergeSetId,
                   dropDeploySetId = dropSetId,
                   mergeDeploySetFinalId = mergeSetFinalId,
                   dropDeploySetFinalId = dropSetFinalId,
                 )

      blockId <- insertIfNot(b.hash, queries.blockIdByHash, newBlock, blockInsert)
    } yield blockId

    actions.transactionally
  }

  /** Get a list of all blocks hashes from DB */
  def blockGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] = queries.blocks.result

  /** Get deploy by unique sig. Returned (TableDeploys.Deploy, shard.name, deployer.pubKey) */
  def blockGetData(
    hash: Array[Byte],
  ): DBIOAction[Option[api.data.Block], NoStream, Read & Transactional] =
    queries
      .blockByHash(hash)
      .result
      .headOption
      .flatMap {
        case Some(block) => getBlockData(block).map(_.some)
        case None        => DBIO.successful(None)
      }
      .transactionally

  private def getBlockData(b: TableBlocks.Block): DBIOAction[Block, NoStream, Read & Transactional] = for {
    validatorPK          <- queries.validatorPkById(b.validatorId).result.head
    shardName            <- queries.shardNameById(b.shardId).result.head
    justificationSetData <- getBlockSetData(b.justificationSetId)
    offencesSetData      <- getBlockSetData(b.offencesSetId)
    bondsMapData         <- getBondsMapData(b.bondsMapId)
    finalFringeData      <- getBlockSetData(b.finalFringeId)
    deploySetData        <- getDeploySetData(b.execDeploySetId)
    mergeSetData         <- getDeploySetData(b.mergeDeploySetId)
    dropSetData          <- getDeploySetData(b.dropDeploySetId)
    mergeSetFinalData    <- getDeploySetData(b.mergeDeploySetFinalId)
    dropSetFinalData     <- getDeploySetData(b.dropDeploySetFinalId)
  } yield api.data.Block(
    version = b.version,
    hash = b.hash,
    sigAlg = b.sigAlg,
    signature = b.signature,
    finalStateHash = b.finalStateHash,
    postStateHash = b.postStateHash,
    validatorPk = validatorPK,
    shardName = shardName,
    justificationSet = justificationSetData,
    seqNum = b.seqNum,
    offencesSet = offencesSetData,
    bondsMap = bondsMapData,
    finalFringe = finalFringeData,
    execDeploySet = deploySetData,
    mergeDeploySet = mergeSetData,
    dropDeploySet = dropSetData,
    mergeDeploySetFinal = mergeSetFinalData,
    dropDeploySetFinal = dropSetFinalData,
  )

  private def getBlockSetData(idOpt: Option[Long]): DBIOAction[Option[SetData], NoStream, Read & Transactional] =
    idOpt match {
      case Some(id) =>
        for {
          hash <- queries.blockSetHashById(id).result.head
          data <- blockSetGetDataById(id)
        } yield Some(api.data.SetData(hash, data))
      case None     => DBIO.successful(None)
    }

  private def getDeploySetData(idOpt: Option[Long]): DBIOAction[Option[SetData], NoStream, Read & Transactional] =
    idOpt match {
      case Some(id) =>
        for {
          hash <- queries.deploySetHashById(id).result.head
          data <- deploySetGetDataById(id)
        } yield Some(api.data.SetData(hash, data))
      case None     => DBIO.successful(None)
    }

  private def getBondsMapData(id: Long): DBIOAction[BondsMapData, NoStream, Read] = for {
    hash <- queries.bondsMapHashById(id).result.head
    data <- getBondsMapDataById(id)
  } yield api.data.BondsMapData(hash, data)

  /** DeploySet */

  /** Get a list of all deploySet hashes from DB */
  def deploySetGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] = queries.deploySets.result

  /** Insert a new deploy set in table if there is no such entry. Returned id */
  def deploySetInsertIfNot(hash: Array[Byte], deploySigs: Seq[Array[Byte]]): DBIOAction[Long, NoStream, All] = {

    def deploySetInsert(hash: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qDeploySets.map(_.hash) returning qDeploySets.map(_.id)) += hash

    def insertBinds(deploySetId: Long, deployIds: Seq[Long]): DBIOAction[Option[Int], NoStream, Write] =
      qDeploySetBinds ++= deployIds.map(TableDeploySetBinds.DeploySetBind(deploySetId, _))

    def insertAllData(in: (Array[Byte], Seq[Array[Byte]])): DBIOAction[Long, NoStream, Write & Read & Transactional] =
      in match {
        case (hash, deploySigs) =>
          (for {
            deploySetId <- deploySetInsert(hash)
            deployIds   <- queries.deployIdsBySigs(deploySigs).result
            _           <- if (deployIds.length == deploySigs.length) insertBinds(deploySetId, deployIds)
                           else
                             DBIO.failed(
                               new RuntimeException(
                                 "Signatures of deploys added to deploy set do not match deploys in deploy table",
                               ),
                             )
          } yield deploySetId).transactionally
      }

    insertIfNot(hash, queries.deploySetIdByHash, (hash, deploySigs), insertAllData)
  }

  /** Get a list of signatures for deploys included at this deploySet. If there isn't such set - return None */
  def deploySetGetData(hash: Array[Byte]): DBIOAction[Option[Seq[Array[Byte]]], NoStream, Read & Transactional] =
    queries
      .deploySetIdByHash(hash)
      .result
      .headOption
      .flatMap {
        case Some(deploySetId) => deploySetGetDataById(deploySetId).map(Some(_))
        case None              => DBIO.successful(None)
      }
      .transactionally

  private def deploySetGetDataById(deploySetId: Long): DBIOAction[Seq[Array[Byte]], NoStream, Read & Transactional] =
    queries.deployIdsByDeploySetId(deploySetId).result.flatMap(queries.deploySigsByIds(_).result).transactionally

  /** BlockSet */

  /** Get a list of all blockSet hashes from DB */
  def blockSetGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] = queries.blockSets.result

  /** Insert a new block set in table if there is no such entry. Returned id */
  def blockSetInsertIfNot(hash: Array[Byte], blockHashes: Seq[Array[Byte]]): DBIOAction[Long, NoStream, All] = {
    def blockSetInsert(hash: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qBlockSets.map(_.hash) returning qBlockSets.map(_.id)) += hash

    def getBlockIdsByHashes(hashes: Seq[Array[Byte]]): DBIOAction[Seq[Long], NoStream, Read] =
      qBlocks.filter(_.hash inSet hashes).map(_.id).result

    def insertBinds(blockSetId: Long, blockIds: Seq[Long]): DBIOAction[Option[Int], NoStream, Write] = {
      val binds = blockIds.map(TableBlockSetBinds.BlockSetBind(blockSetId, _))
      qBlockSetBinds ++= binds
    }

    def insertAllData(in: (Array[Byte], Seq[Array[Byte]])): DBIOAction[Long, NoStream, Write & Read] = in match {
      case (hash, blockHashes) =>
        for {
          blockSetId <- blockSetInsert(hash)
          blockIds   <- getBlockIdsByHashes(blockHashes)
          _          <- if (blockIds.length == blockHashes.length) insertBinds(blockSetId, blockIds)
                        else
                          DBIO.failed(
                            new RuntimeException("Hashes of blocks added to block set do not match blocks in block table"),
                          )
        } yield blockSetId
    }

    insertIfNot(hash, queries.blockSetIdByHash, (hash, blockHashes), insertAllData)
  }

  /** Get a list of hashes for blocks included at this blockSet. If there isn't such set - return None */
  def blockSetGetData(hash: Array[Byte]): DBIOAction[Option[Seq[Array[Byte]]], NoStream, Read & Transactional] =
    queries
      .blockSetIdByHash(hash)
      .result
      .headOption
      .flatMap {
        case Some(blockSetId) => blockSetGetDataById(blockSetId).map(Some(_))
        case None             => DBIO.successful(None)
      }
      .transactionally

  private def blockSetGetDataById(blockSetId: Long): DBIOAction[Seq[Array[Byte]], NoStream, Read & Transactional] =
    queries.blockIdsByBlockSetId(blockSetId).result.flatMap(queries.blockHashesByIds(_).result).transactionally

  /** BondsMap */

  /** Insert a new bonds map in table if there is no such entry. Returned id */
  def bondsMapInsertIfNot(hash: Array[Byte], bMap: Seq[(Array[Byte], Long)]): DBIOAction[Long, NoStream, All] = {

    def bondsMapInsert(hash: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qBondsMaps.map(_.hash) returning qBondsMaps.map(_.id)) += hash

    def bondsInsert(bondsMapId: Long, bMap: Seq[(Long, Long)]): DBIOAction[Option[Int], NoStream, Write] = {
      val bonds = bMap.map { case (validatorId, stake) =>
        TableBonds.Bond(bondsMapId, validatorId, stake)
      }
      qBonds ++= bonds
    }

    def insertAllData(
      in: (Array[Byte], Seq[(Array[Byte], Long)]),
    ): DBIOAction[Long, NoStream, All] =
      in match {
        case (hash, bMapSeq) =>
          for {
            bondsMapId            <- bondsMapInsert(hash)
            validatorsIdWithStake <- DBIO.sequence(bMapSeq.map { case (validatorPk, stake) =>
                                       validatorInsertIfNot(validatorPk).map(validatorId => (validatorId, stake))
                                     })
            _                     <- bondsInsert(bondsMapId, validatorsIdWithStake)
          } yield bondsMapId
      }

    insertIfNot(hash, queries.bondsMapIdByHash, (hash, bMap), insertAllData)
  }

  /** Get a list of all bonds maps hashes from DB */
  def bondsMapGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] = queries.bondsMap.result

  /** Get a bonds map data by map hash. If there isn't such set - return None */
  def bondsMapGetData(hash: Array[Byte]): DBIOAction[Option[Seq[(Array[Byte], Long)]], NoStream, All] =
    queries
      .bondsMapIdByHash(hash)
      .result
      .headOption
      .flatMap {
        case Some(bondsMapId) => getBondsMapDataById(bondsMapId).map(Some(_))
        case None             => DBIO.successful(None)
      }
      .transactionally

  private def getBondsMapDataById(bondsMapId: Long): DBIOAction[Seq[(Array[Byte], Long)], NoStream, Read] = for {
    bondMapIds            <- queries.bondIdsByBondsMapId(bondsMapId).result
    validatorsPkWithStake <-
      DBIO.sequence(bondMapIds.map { case (validatorId, stake) =>
        queries.validatorPkById(validatorId).result.headOption.map(validatorPk => (validatorPk, stake))
      })
  } yield validatorsPkWithStake.collect { case (Some(pk), stake) => (pk, stake) }

  /** Validator */

  private def validatorInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, All] = {
    def validatorInsert(pK: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qValidators.map(_.pubKey) returning qValidators.map(_.id)) += pK

    insertIfNot(pK, queries.validatorIdByPK, pK, validatorInsert)
  }

  /** Insert a new record in table if there is no such entry. Returned id */
  private def insertIfNot[A, B](
    unique: A,
    getIdByUnique: A => Query[Rep[Long], Long, Seq],
    insertable: B,
    insert: B => DBIOAction[Long, NoStream, All],
  ): DBIOAction[Long, NoStream, All] =
    getIdByUnique(unique).result.headOption.flatMap {
      case Some(existingId) => DBIO.successful(existingId)
      case None             => insert(insertable)
    }.transactionally
}
