package slick

import slick.dbio.Effect.{Read, Write}
import slick.jdbc.JdbcProfile
import slick.tables.*
import slick.tables.TableValidators.Validator

import scala.concurrent.ExecutionContext

final case class SlickQuery()(implicit val profile: JdbcProfile, implicit val ec: ExecutionContext) {
  import profile.api.*

  def storeValue(key: String, value: String): DBIOAction[Int, NoStream, Effect.Write] =
    qConfigs.insertOrUpdate((key, value))

  def loadValue(key: String): DBIOAction[Option[String], NoStream, Effect.Read] =
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
  def deployGetData(
    sig: Array[Byte],
  ): DBIOAction[Option[api.data.Deploy], NoStream, Read] = {
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
  def deployInsertIfNot(d: api.data.Deploy): DBIOAction[Long, NoStream, Effect.All] = {
    def deployerInsertIfNot(pK: Array[Byte]) = {
      def deployerIdByPK(pK: Array[Byte]) =
        qDeployers.filter(_.pubKey === pK).map(_.id)
      def deployerInsert(pK: Array[Byte]) =
        (qDeployers.map(_.pubKey) returning qDeployers.map(_.id)) += pK
      insertIfNot(pK, deployerIdByPK, pK, deployerInsert)
    }

    /** Get deploy id by unique signature */
    def deployIdBySig(sig: Array[Byte]) =
      qDeploys.filter(_.sig === sig).map(_.id)

    /** Insert a new record in table. Returned id. */
    def deployInsert(deploy: TableDeploys.Deploy) =
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

  /** DeploySet */
  /** Insert a new deploy set in table if there is no such entry. Returned id */
  def deploySetInsertIfNot(hash: Array[Byte], deploySigs: Seq[Array[Byte]]): DBIOAction[Long, NoStream, Effect.All] = {
    def deploySetIdByHash(hash: Array[Byte]) =
      qDeploySets.filter(_.hash === hash).map(_.id)

    def deploySetInsert(hash: Array[Byte]) =
      (qDeploySets.map(_.hash) returning qDeploySets.map(_.id)) += hash

    def getDeployIdsBySigs(sigs: Seq[Array[Byte]]) =
      qDeploys.filter(_.sig inSet sigs).map(_.id).result

    def insertBinds(deploySetId: Long, deployIds: Seq[Long]) = {
      val binds = deployIds.map(TableDeploySetBinds.DeploySetBind(deploySetId, _))
      qDeploySetBinds ++= binds
    }

    def insertAllData(in: (Array[Byte], Seq[Array[Byte]])) = in match {
      case (hash, deploySigs) =>
        for {
          deploySetId <- deploySetInsert(hash)
          deployIds   <- getDeployIdsBySigs(deploySigs)
          _           <- insertBinds(deploySetId, deployIds)
        } yield deploySetId
    }

    val actions = insertIfNot(hash, deploySetIdByHash, (hash, deploySigs), insertAllData)
    actions.transactionally
  }

  /** Get a list of all deploySet hashes from DB*/
  def deploySetGetAll: DBIOAction[Seq[Array[Byte]], NoStream, Read] =
    qDeploySets.map(_.hash).result

  private def deploySetGetDataById(deploySetId: Long): DBIOAction[Seq[Array[Byte]], NoStream, Read] = {
    def getDeployIds(deploySetId: Long)    =
      qDeploySetBinds.filter(_.deploySetId === deploySetId).map(_.deployId).result
    def getDeploySigsByIds(ids: Seq[Long]) =
      qDeploys.filter(_.id inSet ids).map(_.sig).result

    for {
      ids  <- getDeployIds(deploySetId)
      sigs <- getDeploySigsByIds(ids)
    } yield sigs
  }

  /** Get a list of signatures for deploys included at this deploySet. If there isn't such set - return None*/
  def deploySetGetData(hash: Array[Byte]): DBIOAction[Option[Seq[Array[Byte]]], NoStream, Effect.All] = {
    def deploySetIdByHash(hash: Array[Byte]) =
      qDeploySets.filter(_.hash === hash).map(_.id).result.headOption

    val actions = for {
      deploySetIdOpt <- deploySetIdByHash(hash)
      r              <- deploySetIdOpt match {
                          case Some(deploySetId) => deploySetGetDataById(deploySetId).map(Some(_))
                          case None              => DBIO.successful(None)
                        }
    } yield r

    actions.transactionally
  }

  /** BlockSet */
  /** Insert a new block set in table if there is no such entry. Returned id */
  def blockSetInsertIfNot(hash: Array[Byte], blockHashes: Seq[Array[Byte]]): DBIOAction[Long, NoStream, Effect.All] = {
    def blockSetIdByHash(hash: Array[Byte]) =
      qBlockSets.filter(_.hash === hash).map(_.id)

    def blockSetInsert(hash: Array[Byte]) =
      (qBlockSets.map(_.hash) returning qBlockSets.map(_.id)) += hash

    def getBlockIdsByHashes(hashes: Seq[Array[Byte]]) =
      qBlocks.filter(_.hash inSet hashes).map(_.id).result

    def insertBinds(blockSetId: Long, blockIds: Seq[Long]) = {
      val binds = blockIds.map(TableBlockSetBinds.BlockSetBind(blockSetId, _))
      qBlockSetBinds ++= binds
    }

    def insertAllData(in: (Array[Byte], Seq[Array[Byte]])) = in match {
      case (hash, blockHashes) =>
        for {
          blockSetId <- blockSetInsert(hash)
          blockIds   <- getBlockIdsByHashes(blockHashes)
          _          <- insertBinds(blockSetId, blockIds)
        } yield blockSetId
    }

    val actions = insertIfNot(hash, blockSetIdByHash, (hash, blockHashes), insertAllData)
    actions.transactionally
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
    for {
      ids  <- getBlockIds(blockSetId)
      sigs <- getBlockHashesByIds(ids)
    } yield sigs
  }

  /** Get a list of hashes for blocks included at this blockSet. If there isn't such set - return None */
  def blockSetGetData(hash: Array[Byte]): DBIOAction[Option[Seq[Array[Byte]]], NoStream, Effect.All] = {
    def blockSetIdByHash(hash: Array[Byte]) =
      qBlockSets.filter(_.hash === hash).map(_.id).result.headOption
    val actions                             = for {
      blockSetIdOpt <- blockSetIdByHash(hash)
      r             <- blockSetIdOpt match {
                         case Some(blockSetId) => blockSetGetDataById(blockSetId).map(Some(_))
                         case None             => DBIO.successful(None)
                       }
    } yield r

    actions.transactionally
  }

  /** BondsMap */
  /** Insert a new bonds map in table if there is no such entry. Returned id */
  def bondsMapInsertIfNot(hash: Array[Byte], bMap: Seq[(Array[Byte], Long)]): DBIOAction[Long, NoStream, Effect.All] = {
    def bondsMapIdByHash(hash: Array[Byte]) =
      qBondsMaps.filter(_.hash === hash).map(_.id)

    def bondsMapInsert(hash: Array[Byte]) =
      (qBondsMaps.map(_.hash) returning qBondsMaps.map(_.id)) += hash

    def validatorsInsertIfNot(pKs: Seq[Array[Byte]]) =
      DBIO.sequence(
        pKs.map(validatorInsertIfNot),
      ) // TODO: Can be rewritten in 2 actions, if work with groups of records

    def bondsInsert(bondsMapId: Long, bMap: Seq[(Long, Long)]) = {
      val bonds = bMap.map { case (validatorId, stake) =>
        TableBonds.Bond(bondsMapId, validatorId, stake)
      }
      qBonds ++= bonds
    }

    def insertAllData(in: (Array[Byte], Seq[(Array[Byte], Long)])) = in match {
      case (hash, bMap) =>
        for {
          bondsMapId   <- bondsMapInsert(hash)
          validatorIds <- validatorsInsertIfNot(bMap.map(_._1))
          _            <- bondsInsert(bondsMapId, validatorIds.zip(bMap.map(_._2)))
        } yield bondsMapId
    }

    val actions = insertIfNot(hash, bondsMapIdByHash, (hash, bMap), insertAllData)
    actions.transactionally
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
  def bondsMapGetData(hash: Array[Byte]): DBIOAction[Option[Seq[(Array[Byte], Long)]], NoStream, Effect.All] = {
    def bondsMapIdByHash(hash: Array[Byte]) =
      qBondsMaps.filter(_.hash === hash).map(_.id).result.headOption

    val actions = for {
      bondsMapIdOpt <- bondsMapIdByHash(hash)
      r             <- bondsMapIdOpt match {
                         case Some(bondsMapId) => getBondsMapDataById(bondsMapId).map(Some(_))
                         case None             => DBIO.successful(None)
                       }
    } yield r

    actions.transactionally
  }

  /** Block */
  /** Insert a new Block in table if there is no such entry. Returned id */
  def blockInsertIfNot(b: api.data.Block): DBIOAction[Long, NoStream, Effect.All] = {

    def insertBlockSet(setDataOpt: Option[api.data.SetData]): DBIOAction[Option[Long], NoStream, Effect.All] =
      setDataOpt match {
        case Some(setData) => blockSetInsertIfNot(setData.hash, setData.data).map(Some(_))
        case None          => DBIO.successful(None)
      }

    def insertDeploySet(setDataOpt: Option[api.data.SetData]): DBIOAction[Option[Long], NoStream, Effect.All] =
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
                   finalFringe = finalFringe,
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
  ): DBIOAction[Option[api.data.Block], NoStream, Read] = {
    val blockQuery = for {
      block            <- qBlocks if block.hash === hash
      validator        <- qValidators if validator.id === block.validatorId
      shard            <- qShards if shard.id === block.shardId
      justificationSet <- qBlockSets if block.justificationSetId === justificationSet.id.?
      offencesSet      <- qBlockSets if block.offencesSetId === offencesSet.id.?
      bondsMap         <- qBondsMaps if block.bondsMapId === bondsMap.id
      finalFringe      <- qBlockSets if block.offencesSetId === finalFringe.id.?
      deploySet        <- qDeploySets if block.deploySetId === deploySet.id.?
      finalFringe      <- qBlockSets if block.offencesSetId === finalFringe.id.?
      mergeSet         <- qBlockSets if block.mergeSetId === mergeSet.id.?
      dropSet          <- qBlockSets if block.dropSetId === dropSet.id.?
      mergeSetFinal    <- qBlockSets if block.mergeSetFinalId === mergeSetFinal.id.?
      dropSetFinal     <- qBlockSets if block.dropSetFinalId === dropSetFinal.id.?
    } yield (
      block,
      validator.pubKey,
      shard.name,
      justificationSet.hash.?,
      offencesSet.hash.?,
      bondsMap.hash,
      finalFringe.hash.?,
      deploySet.hash.?,
      mergeSet.hash.?,
      dropSet.hash.?,
      mergeSetFinal.hash.?,
      dropSetFinal.hash.?,
    )

    def getBlockSetData(idOpt: Option[Long], hashOpt: Option[Array[Byte]]) = idOpt match {
      case Some(id) => blockSetGetDataById(id).map(data => Some(api.data.SetData(hashOpt.get, data)))
      case None     => DBIO.successful(None)
    }

    def getDeploySetData(idOpt: Option[Long], hashOpt: Option[Array[Byte]]) = idOpt match {
      case Some(id) => deploySetGetDataById(id).map(data => Some(api.data.SetData(hashOpt.get, data)))
      case None     => DBIO.successful(None)
    }

    def getBondsMapData(id: Long, hash: Array[Byte]) =
      getBondsMapDataById(id).map(data => api.data.BondsMapData(hash, data))

    for {
      blockDataOpt <- blockQuery.result.headOption
      result       <- blockDataOpt match {
                        case Some(
                              (
                                b,
                                validatorPK,
                                shardName,
                                justificationSetHash,
                                offencesSetHash,
                                bondsMapHash,
                                finalFringeHash,
                                deploySetHash,
                                mergeSetHash,
                                dropSetHash,
                                mergeSetFinalHash,
                                dropSetFinalHash,
                              ),
                            ) =>
                          for {
                            justificationSetData <- getBlockSetData(b.justificationSetId, justificationSetHash)
                            offencesSetData      <- getBlockSetData(b.offencesSetId, offencesSetHash)
                            bondsMapData         <- getBondsMapData(b.bondsMapId, bondsMapHash)
                            finalFringeData      <- getBlockSetData(b.finalFringe, finalFringeHash)
                            deploySetData        <- getDeploySetData(b.deploySetId, deploySetHash)
                            mergeSetData         <- getBlockSetData(b.mergeSetId, mergeSetHash)
                            dropSetData          <- getBlockSetData(b.dropSetId, dropSetHash)
                            mergeSetFinalData    <- getBlockSetData(b.mergeSetFinalId, mergeSetFinalHash)
                            dropSetFinalData     <- getBlockSetData(b.dropSetFinalId, dropSetFinalHash)
                          } yield Some(
                            api.data.Block(
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
                            ),
                          )
                        case None => DBIO.successful(None)
                      }
    } yield result
  }

  private def validatorInsertIfNot(pK: Array[Byte]): DBIOAction[Long, NoStream, Effect.All] = {
    def validatorIdByPK(pK: Array[Byte]) =
      qValidators.filter(_.pubKey === pK).map(_.id)

    def validatorInsert(pK: Array[Byte]) =
      (qValidators.map(_.pubKey) returning qValidators.map(_.id)) += pK

    insertIfNot(pK, validatorIdByPK, pK, validatorInsert)
  }

  private def shardInsertIfNot(name: String) = {
    def shardIdByName(name: String) =
      qShards.filter(_.name === name).map(_.id)

    def shardInsert(name: String) =
      (qShards.map(_.name) returning qShards.map(_.id)) += name

    insertIfNot(name, shardIdByName, name, shardInsert)
  }

  /** Insert a new record in table if there is no such entry. Returned id */
  private def insertIfNot[A, B](
    unique: A,
    getIdByUnique: A => Query[Rep[Long], Long, Seq],
    insertable: B,
    insert: B => DBIOAction[Long, NoStream, Effect.All],
  ): DBIOAction[Long, NoStream, Effect.All] = {
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
