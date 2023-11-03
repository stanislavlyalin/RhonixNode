package slick.api.data

final case class Block(
  version: Int,                      // protocol versions
  hash: Array[Byte],                 // strong hash of block content
  sigAlg: String,                    // signature of a hash
  signature: Array[Byte],            // signing algorithm
  finalStateHash: Array[Byte],       // proof of final state
  postStateHash: Array[Byte],        // proof of pre state
  validatorPk: Array[Byte],          // validator public key
  shardName: String,                 // name of a shard
  justificationSet: Option[SetData], // justification set
  seqNum: Long,                      // sequence number
  offencesSet: Option[SetData],      // offences set
  bondsMap: BondsMapData,            // bonds map
  finalFringe: Option[SetData],      // final fringe set
  deploySet: Option[SetData],        // deploy set in the block
  mergeSet: Option[SetData],         // deploy set merged into pre state
  dropSet: Option[SetData],          // deploy set rejected from pre state
  mergeSetFinal: Option[SetData],    // deploy set finally accepted
  dropSetFinal: Option[SetData],     // deploy set finally rejected
)

final case class SetData(
  hash: Array[Byte],
  data: Seq[Array[Byte]],
)

final case class BondsMapData(
  hash: Array[Byte],
  data: Seq[(Array[Byte], Long)],
)
