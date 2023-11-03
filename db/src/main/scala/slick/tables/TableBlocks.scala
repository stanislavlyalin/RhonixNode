package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableBlocks.Block

class TableBlocks(tag: Tag) extends Table[Block](tag, "block") {
  def id: Rep[Long]          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def version: Rep[Int]      = column[Int]("version")
  def hash: Rep[Array[Byte]] = column[Array[Byte]]("hash")

  def sigAlg: Rep[String]         = column[String]("sig_alg")
  def signature: Rep[Array[Byte]] = column[Array[Byte]]("signature")

  def finalStateHash: Rep[Array[Byte]] = column[Array[Byte]]("final_state_hash")
  def postStateHash: Rep[Array[Byte]]  = column[Array[Byte]]("post_state_hash")

  def validatorId: Rep[Long]                = column[Long]("validator_id")
  def shardId: Rep[Long]                    = column[Long]("shard_id")
  def justificationSetId: Rep[Option[Long]] = column[Option[Long]]("justification_set_id")
  def seqNum: Rep[Long]                     = column[Long]("seq_num")
  def offencesSetId: Rep[Option[Long]]      = column[Option[Long]]("offences_set")

  def bondsMapId: Rep[Long]              = column[Long]("bonds_map_id")
  def finalFringeId: Rep[Option[Long]]   = column[Option[Long]]("final_fringe")
  def deploySetId: Rep[Option[Long]]     = column[Option[Long]]("deploy_set_id")
  def mergeSetId: Rep[Option[Long]]      = column[Option[Long]]("merge_set_id")
  def dropSetId: Rep[Option[Long]]       = column[Option[Long]]("drop_set_id")
  def mergeSetFinalId: Rep[Option[Long]] = column[Option[Long]]("merge_set_final_id")
  def dropSetFinalId: Rep[Option[Long]]  = column[Option[Long]]("drop_set_final_id")

  def fk1 = foreignKey("fk_block_validator_id", validatorId, slick.qValidators)(_.id)
  def fk2 = foreignKey("fk_block_shard_id", shardId, slick.qShards)(_.id)
  def fk3 = foreignKey("fk_block_justification_set_id", justificationSetId, slick.qBlockSets)(_.id.?)
  def fk4 = foreignKey("fk_block_offences_set_id", offencesSetId, slick.qBlockSets)(_.id.?)

  def fk5  = foreignKey("fk_block_bonds_map_id", bondsMapId, slick.qBondsMaps)(_.id)
  def fk6  = foreignKey("fk_block_final_fringe_id", finalFringeId, slick.qBlockSets)(_.id.?)
  def fk7  = foreignKey("fk_block_deploy_set_id", deploySetId, slick.qDeploySets)(_.id.?)
  def fk8  = foreignKey("fk_block_merge_set_id", mergeSetId, slick.qDeploySets)(_.id.?)
  def fk9  = foreignKey("fk_block_drop_set_id", dropSetId, slick.qDeploySets)(_.id.?)
  def fk10 = foreignKey("fk_block_merge_set_final_id", mergeSetFinalId, slick.qDeploySets)(_.id.?)
  def fk11 = foreignKey("fk_block_drop_set_final_id", dropSetFinalId, slick.qDeploySets)(_.id.?)

  def idx = index("idx_block", hash, unique = true)

  def * : ProvenShape[Block] = (
    id,
    version,
    hash,
    sigAlg,
    signature,
    finalStateHash,
    postStateHash,
    validatorId,
    shardId,
    justificationSetId,
    seqNum,
    offencesSetId,
    bondsMapId,
    finalFringeId,
    deploySetId,
    mergeSetId,
    dropSetId,
    mergeSetFinalId,
    dropSetFinalId,
  ).mapTo[Block]
}

object TableBlocks {
  final case class Block(
    id: Long,          // primary key
    version: Int,      // protocol versions
    hash: Array[Byte], // strong hash of block content

    // Signature
    sigAlg: String,         // signature of a hash
    signature: Array[Byte], // signing algorithm

    // On Chain state
    finalStateHash: Array[Byte], // proof of final state
    postStateHash: Array[Byte],  // proof of pre state

    // pointers to inner data
    validatorId: Long,                // pointer to validator
    shardId: Long,                    // pointer to shard
    justificationSetId: Option[Long], // pointer to justification set
    seqNum: Long,                     // sequence number
    offencesSetId: Option[Long],      // pointer to offences set

    // these are optimisations/data to short circuit validation
    bondsMapId: Long,              // pointer to bonds map
    finalFringeId: Option[Long],   // pointer to final fringe set
    deploySetId: Option[Long],     // pointer to deploy set in the block
    mergeSetId: Option[Long],      // pointer to deploy set merged into pre state
    dropSetId: Option[Long],       // pointer to deploy set rejected from pre state
    mergeSetFinalId: Option[Long], // pointer to deploy set finally accepted
    dropSetFinalId: Option[Long],  // pointer to deploy set finally rejected
  )
}
