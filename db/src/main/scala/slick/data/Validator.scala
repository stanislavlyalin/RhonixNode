package slick.data

final case class Validator(
  id: Long,           // primary key
  pubKey: Array[Byte],// validator public key
)
