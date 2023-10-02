package slick.data

final case class BlockSet(
  id: Long,           // primary key
  hash: Array[Byte],  // strong hash of a hashes of blocks in a set (global identifier)
  blockIds: Set[Long],// pointers to blocks
)
