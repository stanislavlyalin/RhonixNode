package sdk.api

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

final case class BlockBonds(blockId: Long, bondId: Long)

final case class BlockDeploys(blockId: Long, deployId: Long)

final case class BlockJustifications(validatorId: Long, latestBlockId: Long)

final case class Bond(validator: Validator, stake: Long)

final case class Deploy(
  hash: Array[Byte],
  publicKey: Array[Byte],
  shardId: String,
  program: String,
  phloPrice: Long,
  phloLimit: Long,
  timestamp: Long,
  validAfterBlockNumber: Long,
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Deploy =>
      this.hash.sameElements(that.hash) &&
      this.publicKey.sameElements(that.publicKey) &&
      this.shardId == that.shardId &&
      this.program == that.program &&
      this.phloPrice == that.phloPrice &&
      this.phloLimit == that.phloLimit &&
      this.timestamp == that.timestamp &&
      this.validAfterBlockNumber == that.validAfterBlockNumber
    case _            => false
  }
}

final case class Justification(
  id: Long,
  validator: Validator,
  lastestBlock: Block,
)

final case class Validator(
  publicKey: Array[Byte], // Unique index
  http: String,
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Validator =>
      this.publicKey.sameElements(that.publicKey) && this.http == that.http
    case _               => false
  }
}
