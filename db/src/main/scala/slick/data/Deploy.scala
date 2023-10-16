package slick.data

final case class Deploy(
  id: Long,         // primary key
  sig: Array[Byte], // deploy signature
  deployerId: Long, // pointer to a deployer
  shardId: Long,    // pointer to a shard
  program: String,  // code of the program
  phloPrice: Long,  // price offered for phlogiston
  phloLimit: Long,  // limit offered for execution
  nonce: Long,      // nonce of a deploy
)
