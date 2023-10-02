package slick.data

final case class Deployer(
  id: Long,           // primary key
  pubKey: Array[Byte],// public key of a deployer
)
