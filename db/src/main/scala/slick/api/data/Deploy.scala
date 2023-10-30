package slick.api.data

final case class Deploy(
  sig: Array[Byte],        // deploy signature
  deployerPk: Array[Byte], // deployer public key
  shardName: String,       // unique name of a shard
  program: String,         // code of the program
  phloPrice: Long,         // price offered for phlogiston
  phloLimit: Long,         // limit offered for execution
  nonce: Long,             // nonce of a deploy
)
