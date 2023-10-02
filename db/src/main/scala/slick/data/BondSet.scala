package slick.data

final case class BondSet(
  id: Long,          // primary key
  hash: Array[Byte], // strong hash of a bonds set content (global identifier)
  bonds: List[Long], // pointers to bonds
)
