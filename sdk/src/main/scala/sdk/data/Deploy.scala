package sdk.data

import sdk.primitive.ByteArray

final case class Deploy(
  id: Long,            // primary key
  sig: ByteArray,      // deploy signature
  deployer: ByteArray, // pointer to a deployer
  shardId: String,     // pointer to a shard
  program: String,     // code of the program
  phloPrice: Long,     // price offered for phlogiston
  phloLimit: Long,     // limit offered for execution
  nonce: Long,         // nonce of a deploy
) {

  override def equals(obj: Any): Boolean = obj match {
    case that: Deploy => this.id == that.id
    case _            => false
  }

  override def hashCode(): Int = id.hashCode()
}
