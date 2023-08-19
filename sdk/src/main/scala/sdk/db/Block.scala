package sdk.db

import sdk.DbTable

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class BlockTable(
  id: Long,
  hash: Array[Byte],
  senderId: Long,
  version: Int,
  shardId: String,
  seqNum: Long,
  number: Long,
  justificationsHash: Array[Byte], // hash(Set[Justification]),
  bondsHash: Array[Byte],          // hash(Map[Validator, Long]),

  // Rholang (tuple space) state change
  preStateHash: Array[Byte],  // hash(VM state)
  postStateHash: Array[Byte], // hash(VM state)
  deploysHash: Array[Byte],   // hash(Set[Deploy])

  // Block signature
  signatureAlg: String,
  signature: Array[Byte],

  // Status
  status: Int,
) extends DbTable

final case class Block(
  hash: Array[Byte],
  sender: Validator,
  version: Int,
  shardId: String,
  seqNum: Long,
  number: Long,
  justificationsHash: Array[Byte], // hash(Set[Justification]),
  justifications: Set[Validator],
  bondsHash: Array[Byte],          // hash(Map[Validator, Long]),
  bonds: Set[Bond],

  // Rholang (tuple space) state change
  preStateHash: Array[Byte],  // hash(VM state)
  postStateHash: Array[Byte], // hash(VM state)
  deploysHash: Array[Byte],   // hash(Set[Deploy])
  deploys: Set[Deploy],

  // Block signature
  signatureAlg: String,
  signature: Array[Byte],

  // Status
  status: Int,
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Block =>
      this.hash.sameElements(that.hash) &&
      this.sender == that.sender &&
      this.version == that.version &&
      this.shardId == that.shardId &&
      this.seqNum == that.seqNum &&
      this.number == that.number &&
      this.justificationsHash.sameElements(that.justificationsHash) &&
      this.justifications == that.justifications &&
      this.bondsHash.sameElements(that.bondsHash) &&
      this.bonds == that.bonds &&
      this.preStateHash.sameElements(that.preStateHash) &&
      this.postStateHash.sameElements(that.postStateHash) &&
      this.deploysHash.sameElements(that.deploysHash) &&
      this.deploys == this.deploys &&
      this.signatureAlg == this.signatureAlg &&
      this.signature.sameElements(that.signature) &&
      this.status == that.status
    case _           => false
  }
}

object Block {
  def toDb(id: Long, block: Block, senderId: Long): BlockTable = BlockTable(
    id,
    block.hash,
    senderId,
    block.version,
    block.shardId,
    block.seqNum,
    block.number,
    block.justificationsHash,
    block.bondsHash,
    block.preStateHash,
    block.postStateHash,
    block.deploysHash,
    block.signatureAlg,
    block.signature,
    block.status,
  )

  def fromDb(
    block: BlockTable,
    sender: Validator,
    justifications: Set[Validator],
    bonds: Set[Bond],
    deploys: Set[Deploy],
  ): Block = Block(
    block.hash,
    sender,
    block.version,
    block.shardId,
    block.seqNum,
    block.number,
    block.justificationsHash,
    justifications,
    block.bondsHash,
    bonds,
    block.preStateHash,
    block.postStateHash,
    block.deploysHash,
    deploys,
    block.signatureAlg,
    block.signature,
    block.status,
  )
}

trait BlockDbApi[F[_]] {
  def insert(block: Block, senderId: Long): F[Long]
  def update(id: Long, block: Block, senderId: Long): F[Unit]

  def getById(id: Long): F[Option[Block]]
  def getByHash(hash: Array[Byte]): F[Option[Block]]
}
