package sdk.data

import sdk.primitive.ByteArray

final case class Block(
  id: Long,        // primary key
  version: Int,    // protocol versions
  hash: ByteArray, // strong hash of block content

  // Signature
  sigAlg: String,       // signature of a hash
  signature: ByteArray, // signing algorithm

  // On Chain state
  finalStateHash: ByteArray, // proof of final state
  postStateHash: ByteArray,  // proof of pre state

  // pointers to inner data
  validatorId: ByteArray,           // pointer to validator
  shardId: String,                  // pointer to shard
  justificationSet: Set[ByteArray], // pointer to justification set
  seqNum: Long,                     // sequence number
  offencesSet: Set[ByteArray],      // pointer to offences set

  bondsMap: Map[ByteArray, Long],          // pointer to bonds map
  finalFringe: Set[ByteArray],             // pointer to final fringe set
  deploySetId: Set[ByteArray],             // pointer to deploy set in the block
  mergeSetId: Set[ByteArray],              // pointer to deploy set merged into pre state
  dropSetId: Set[ByteArray],               // pointer to deploy set rejected from pre state
  mergeSetFinalId: Option[Set[ByteArray]], // pointer to deploy set finally accepted
  dropSetFinalId: Option[Set[ByteArray]],  // pointer to deploy set finally rejected
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Block => this.id == that.id
    case _           => false
  }

  override def hashCode(): Int = id.hashCode()
}
