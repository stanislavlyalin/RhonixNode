package slick.data

final case class Bond(
  id: Long,          // primary key
  validatorId: Long, // pointer to a validator
  stake: Long,       // stake of a validator
)
