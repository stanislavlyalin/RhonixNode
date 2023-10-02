package slick.data

final case class DeploySet(
  id: Long,            // primary key
  hash: Array[Byte],   // strong hash of a signatures of deploys in a set (global identifier)
  deployIds: Set[Long],// pointers to deploys
)
