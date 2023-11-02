package slick

import cats.syntax.all.*
import slick.dbio.Effect.*
import slick.jdbc.JdbcProfile
import slick.sql.{FixedSqlAction, SqlAction}
import slick.tables.*

import scala.concurrent.ExecutionContext.Implicits.global

final case class SlickQuery(profile: JdbcProfile) {
  import profile.api.*

  def putConfig(key: String, value: String): DBIOAction[Int, NoStream, Write] =
    qConfigs.insertOrUpdate((key, value))

  def getConfig(key: String): DBIOAction[Option[String], NoStream, Read] =
    qConfigs.filter(_.name === key).map(_.value).result.headOption

  /** Deploy */
  /** Get a list of all shard names */
  def shardGetAll: DBIOAction[Seq[String], NoStream, Read] =
    qShards.map(_.name).result

  /** Get a list of all deployer public keys*/
  def deployerGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] =
    qDeployers.map(_.pubKey).result

  /** Get a list of all deploy signatures */
  def deployGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] =
    qDeploys.map(_.sig).result

  /** Get deploy by unique sig. Returned (TableDeploys.Deploy, shard.name, deployer.pubKey)*/
  def deployGetData(sig: Array[Byte]): DBIOAction[Option[api.data.Deploy], NoStream, Read] = {
    val query = for {
      deploy   <- qDeploys if deploy.sig === sig
      shard    <- qShards if shard.id === deploy.shardId
      deployer <- qDeployers if deployer.id === deploy.deployerId
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
    def deployerInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, All] = {
      def deployerIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq]       =
        qDeployers.filter(_.pubKey === pK).map(_.id)
      def deployerInsert(pK: Array[Byte]): DBIOAction[Long, NoStream, Write] =
        (qDeployers.map(_.pubKey) returning qDeployers.map(_.id)) += pK
      insertIfNot(pK, deployerIdByPK, pK, deployerInsert)
    }

    /** Get deploy id by unique signature */
    def deployIdBySig(sig: Array[Byte]): Query[Rep[Long], Long, Seq] =
      qDeploys.filter(_.sig === sig).map(_.id)

    /** Insert a new record in table. Returned id. */
    def deployInsert(deploy: TableDeploys.Deploy): FixedSqlAction[Long, NoStream, Write] =
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
      deployId   <- insertIfNot(d.sig, deployIdBySig, newDeploy, deployInsert)
    } yield deployId

    actions.transactionally
  }

  /** Delete deploy by unique sig. And clean up dependencies in Deployers and Shards if possible.
   * Return 1 if deploy deleted, or 0 otherwise. */
  def deployDeleteAndCleanUp(sig: Array[Byte]): DBIOAction[Int, NoStream, All] = {
    def deployerForCleanUp(deployerId: Long): Query[TableDeployers, TableDeployers.Deployer, Seq] = {
      val relatedDeploysExist = qDeploys.filter(_.deployerId === deployerId).exists
      qDeployers.filter(d => (d.id === deployerId) && !relatedDeploysExist)
    }

    def shardForCleanUp(shardId: Long): Query[TableShards, TableShards.Shard, Seq] = {
      val relatedDeploysExist = qDeploys.filter(d => d.shardId === shardId).exists
      qShards.filter(s => (s.id === shardId) && !relatedDeploysExist)
    }

    def deployById(id: Long): Query[TableDeploys, TableDeploys.Deploy, Seq] = qDeploys.filter(_.id === id)

    def deleteAndCleanUp(deployId: Long, deployerId: Long, shardId: Long): DBIOAction[Int, NoStream, Write] = for {
      r <- deployById(deployId).delete
      _ <- deployerForCleanUp(deployerId).delete
      _ <- shardForCleanUp(shardId).delete
    } yield r

    def deployIdsBySig(sig: Array[Byte]): Query[(Rep[Long], Rep[Long], Rep[Long]), (Long, Long, Long), Seq] =
      qDeploys.filter(_.sig === sig).map(d => (d.id, d.deployerId, d.shardId))

    deployIdsBySig(sig).result.headOption.flatMap {
      case Some((deployId, deployerId, shardId)) => deleteAndCleanUp(deployId, deployerId, shardId)
      case None                                  => DBIO.successful(0)
    }.transactionally
  }

  /** DeploySet */
  /** Insert a new deploy set in table if there is no such entry. Returned id */
  def deploySetInsertIfNot(hash: Array[Byte], deploySigs: Seq[Array[Byte]]): DBIOAction[Long, NoStream, All] = {
    def deploySetIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] =
      qDeploySets.filter(_.hash === hash).map(_.id)

    def deploySetInsert(hash: Array[Byte]): FixedSqlAction[Long, NoStream, Write] =
      (qDeploySets.map(_.hash) returning qDeploySets.map(_.id)) += hash

    def getDeployIdsBySigs(sigs: Seq[Array[Byte]]): Query[Rep[Long], Long, Seq] =
      qDeploys.filter(_.sig inSet sigs).map(_.id)

    def insertBinds(deploySetId: Long, deployIds: Seq[Long]): FixedSqlAction[Option[Int], NoStream, Write] =
      qDeploySetBinds ++= deployIds.map(TableDeploySetBinds.DeploySetBind(deploySetId, _))

    def insertAllData(in: (Array[Byte], Seq[Array[Byte]])): DBIOAction[Long, NoStream, Write & Read & Transactional] =
      in match {
        case (hash, deploySigs) =>
          (for {
            deploySetId <- deploySetInsert(hash)
            deployIds   <- getDeployIdsBySigs(deploySigs).result
            _           <- insertBinds(deploySetId, deployIds)
          } yield deploySetId).transactionally
      }

    insertIfNot(hash, deploySetIdByHash, (hash, deploySigs), insertAllData)
  }

  /** Get a list of all deploySet hashes from DB*/
  def deploySetGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] =
    qDeploySets.map(_.hash).result

  private def deploySetGetDataById(deploySetId: Long): DBIOAction[Seq[Array[Byte]], NoStream, Read & Transactional] = {
    def getDeployIds(deploySetId: Long)    =
      qDeploySetBinds.filter(_.deploySetId === deploySetId).map(_.deployId).result
    def getDeploySigsByIds(ids: Seq[Long]) =
      qDeploys.filter(_.id inSet ids).map(_.sig).result

    getDeployIds(deploySetId).flatMap(getDeploySigsByIds).transactionally
  }

  /** Get a list of signatures for deploys included at this deploySet. If there isn't such set - return None*/
  def deploySetGetData(hash: Array[Byte]): DBIOAction[Option[Seq[Array[Byte]]], NoStream, Read & Transactional] = {
    def deploySetIdByHash(hash: Array[Byte]): SqlAction[Option[Long], NoStream, Read] =
      qDeploySets.filter(_.hash === hash).map(_.id).result.headOption

      deploySetIdByHash(hash).flatMap {
        case Some(deploySetId) => deploySetGetDataById(deploySetId).map(Some(_))
        case None              => DBIO.successful(None)
      }.transactionally
  }

  /** BlockSet */
  /** Insert a new block set in table if there is no such entry. Returned id */
  def blockSetInsertIfNot(hash: Array[Byte], blockHashes: Seq[Array[Byte]]): DBIOAction[Long, NoStream, All] = {
    def blockSetIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] =
      qBlockSets.filter(_.hash === hash).map(_.id)

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
          _          <- insertBinds(blockSetId, blockIds)
        } yield blockSetId
    }

    insertIfNot(hash, blockSetIdByHash, (hash, blockHashes), insertAllData)
  }

  /** Get a list of all blockSet hashes from DB*/
  def blockSetGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] =
    qBlockSets.map(_.hash).result

  private def blockSetGetDataById(
    blockSetId: Long,
  ): DBIOAction[Seq[Array[Byte]], NoStream, Read] = {
    def getBlockIds(blockSetId: Long)       =
      qBlockSetBinds.filter(_.blockSetId === blockSetId).map(_.blockId).result
    def getBlockHashesByIds(ids: Seq[Long]) =
      qBlocks.filter(_.id inSet ids).map(_.hash).result

    getBlockIds(blockSetId).flatMap(getBlockHashesByIds)
  }

  /** Get a list of hashes for blocks included at this blockSet. If there isn't such set - return None */
  def blockSetGetData(hash: Array[Byte]): DBIOAction[Option[Seq[Array[Byte]]], NoStream, Read & Transactional] = {
    def blockSetIdByHash(hash: Array[Byte]): SqlAction[Option[Long], NoStream, Read] =
      qBlockSets.filter(_.hash === hash).map(_.id).result.headOption

    blockSetIdByHash(hash).flatMap {
      case Some(blockSetId) => blockSetGetDataById(blockSetId).map(Some(_))
      case None             => DBIO.successful(None)
    }.transactionally
  }

  /** BondsMap */
  /** Insert a new bonds map in table if there is no such entry. Returned id */
  def bondsMapInsertIfNot(hash: Array[Byte], bMap: Seq[(Array[Byte], Long)]): DBIOAction[Long, NoStream, All] = {
    def bondsMapIdByHash(hash: Array[Byte]): Query[Rep[Long], Long, Seq] =
      qBondsMaps.filter(_.hash === hash).map(_.id)

    def bondsMapInsert(hash: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qBondsMaps.map(_.hash) returning qBondsMaps.map(_.id)) += hash

    def validatorsInsertIfNot(pKs: Seq[Array[Byte]]): DBIOAction[Seq[Long], NoStream, All] =
      DBIO
        .sequence(pKs.map(validatorInsertIfNot))
        .transactionally // TODO: Can be rewritten in 2 actions, if work with groups of records

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
        case (hash, bMap) =>
          for {
            bondsMapId   <- bondsMapInsert(hash)
            validatorIds <- validatorsInsertIfNot(bMap.map(_._1))
            _            <- bondsInsert(bondsMapId, validatorIds.zip(bMap.map(_._2)))
          } yield bondsMapId
      }

    insertIfNot(hash, bondsMapIdByHash, (hash, bMap), insertAllData)
  }

  /** Get a list of all bonds maps hashes from DB */
  def bondsMapGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] =
    qBondsMaps.map(_.hash).result

  private def getBondsMapDataById(bondsMapId: Long): DBIOAction[Seq[(Array[Byte], Long)], NoStream, Read] = {
    def getBondIds(bondsMapId: Long)         =
      qBonds.filter(_.bondsMapId === bondsMapId).map(b => (b.validatorId, b.stake)).result
    def getValidatorPKsByIds(ids: Seq[Long]) =
      qValidators.filter(_.id inSet ids).map(_.pubKey).result

    for {
      bondMapIds            <- getBondIds(bondsMapId)
      (validatorIds, stakes) = bondMapIds.unzip
      validatorPks          <- getValidatorPKsByIds(validatorIds)
    } yield validatorPks zip stakes
  }

  /** Get a bonds map data by map hash. If there isn't such set - return None */
  def bondsMapGetData(hash: Array[Byte]): DBIOAction[Option[Seq[(Array[Byte], Long)]], NoStream, All] = {
    def bondsMapIdByHash(hash: Array[Byte]): SqlAction[Option[Long], NoStream, Read] =
      qBondsMaps.filter(_.hash === hash).map(_.id).result.headOption

    bondsMapIdByHash(hash).flatMap {
      case Some(bondsMapId) => getBondsMapDataById(bondsMapId).map(Some(_))
      case None             => DBIO.successful(None)
    }.transactionally
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

    def blockIdByHash(hash: Array[Byte])      =
      qBlocks.filter(_.hash === hash).map(_.id)
    def blockInsert(block: TableBlocks.Block) =
      (qBlocks returning qBlocks.map(_.id)) += block

    val actions = for {
      validatorId <- validatorInsertIfNot(b.validatorPk)
      shardId     <- shardInsertIfNot(b.shardName)

      justificationSetId <- insertBlockSet(b.justificationSet)
      offencesSet        <- insertBlockSet(b.offencesSet)
      bondsMapId         <- bondsMapInsertIfNot(b.bondsMap.hash, b.bondsMap.data)
      finalFringe        <- insertBlockSet(b.finalFringe)
      deploySetId        <- insertDeploySet(b.deploySet)

      mergeSetId      <- insertBlockSet(b.mergeSet)
      dropSetId       <- insertBlockSet(b.dropSet)
      mergeSetFinalId <- insertBlockSet(b.mergeSetFinal)
      dropSetFinalId  <- insertBlockSet(b.dropSetFinal)

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
                   deploySetId = deploySetId,
                   mergeSetId = mergeSetId,
                   dropSetId = dropSetId,
                   mergeSetFinalId = mergeSetFinalId,
                   dropSetFinalId = dropSetFinalId,
                 )

      blockId <- insertIfNot(b.hash, blockIdByHash, newBlock, blockInsert)
    } yield blockId

    actions.transactionally
  }

  /** Get a list of all blocks hashes from DB */
  def blockGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] =
    qBlocks.map(_.hash).result

  /** Get deploy by unique sig. Returned (TableDeploys.Deploy, shard.name, deployer.pubKey) */
  def blockGetData(
    hash: Array[Byte],
  ): DBIOAction[Option[api.data.Block], NoStream, Read & Transactional] = {
    def getBlock(hash: Array[Byte])           = qBlocks.filter(_.hash === hash).result.headOption
    def getValidatorPk(validatorId: Long)     = qValidators.filter(_.id === validatorId).map(_.pubKey).result.head
    def getShardName(shardId: Long)           = qShards.filter(_.id === shardId).map(_.name).result.head
    def getBlockSetHash(id: Long)             = qBlockSets.filter(_.id === id).map(_.hash).result.head
    def getDeploySetHash(id: Long)            = qDeploySets.filter(_.id === id).map(_.hash).result.head
    def getBondsMapHash(id: Long)             = qBondsMaps.filter(_.id === id).map(_.hash).result.head
    def getBlockSetData(idOpt: Option[Long])  = idOpt match {
      case Some(id) =>
        for {
          hash <- getBlockSetHash(id)
          data <- blockSetGetDataById(id)
        } yield Some(api.data.SetData(hash, data))
      case None     => DBIO.successful(None)
    }
    def getDeploySetData(idOpt: Option[Long]) = idOpt match {
      case Some(id) =>
        for {
          hash <- getDeploySetHash(id)
          data <- deploySetGetDataById(id)
        } yield Some(api.data.SetData(hash, data))
      case None     => DBIO.successful(None)
    }
    def getBondsMapData(id: Long)             = for {
      hash <- getBondsMapHash(id)
      data <- getBondsMapDataById(id)
    } yield api.data.BondsMapData(hash, data)

    def getBlockData(b: TableBlocks.Block) = for {
      validatorPK          <- getValidatorPk(b.validatorId)
      shardName            <- getShardName(b.shardId)
      justificationSetData <- getBlockSetData(b.justificationSetId)
      offencesSetData      <- getBlockSetData(b.offencesSetId)
      bondsMapData         <- getBondsMapData(b.bondsMapId)
      finalFringeData      <- getBlockSetData(b.finalFringeId)
      deploySetData        <- getDeploySetData(b.deploySetId)
      mergeSetData         <- getBlockSetData(b.mergeSetId)
      dropSetData          <- getBlockSetData(b.dropSetId)
      mergeSetFinalData    <- getBlockSetData(b.mergeSetFinalId)
      dropSetFinalData     <- getBlockSetData(b.dropSetFinalId)
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
      deploySet = deploySetData,
      mergeSet = mergeSetData,
      dropSet = dropSetData,
      mergeSetFinal = mergeSetFinalData,
      dropSetFinal = dropSetFinalData,
    )

    getBlock(hash).flatMap {
      case Some(block) => getBlockData(block).map(_.some)
      case None        => DBIO.successful(None)
    }
  }

  private def validatorInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, All] = {
    def validatorIdByPK(pK: Array[Byte]): Query[Rep[Long], Long, Seq] =
      qValidators.filter(_.pubKey === pK).map(_.id)

    def validatorInsert(pK: Array[Byte]): DBIOAction[Long, NoStream, Write] =
      (qValidators.map(_.pubKey) returning qValidators.map(_.id)) += pK

    insertIfNot(pK, validatorIdByPK, pK, validatorInsert)
  }

  private def shardInsertIfNot(name: String): DBIOAction[Long, NoStream, All] = {
    def shardIdByName(name: String): Query[Rep[Long], Long, Seq] =
      qShards.filter(_.name === name).map(_.id)

    def shardInsert(name: String): DBIOAction[Long, NoStream, Write] =
      (qShards.map(_.name) returning qShards.map(_.id)) += name

    insertIfNot(name, shardIdByName, name, shardInsert)
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
