package slick.data

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
  offencesSet: Option[Long],        // pointer to offences set

  // these are optimisations/data to short circuit validation
  bondsMapId: Long,              // pointer to bonds map
  finalFringe: Option[Long],     // pointer to final fringe set
  deploySetId: Option[Long],     // pointer to deploy set in the block
  mergeSetId: Option[Long],      // pointer to deploy set merged into pre state
  dropSetId: Option[Long],       // pointer to deploy set rejected from pre state
  mergeSetFinalId: Option[Long], // pointer to deploy set finally accepted
  dropSetFinalId: Option[Long],  // pointer to deploy set finally rejected
)
