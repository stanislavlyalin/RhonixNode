package sdk.db

import sdk.DbTable

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class ValidatorTable(
  id: Long,
  publicKey: Array[Byte], // Unique index
  http: String,
) extends DbTable

final case class Validator(
  publicKey: Array[Byte], // Unique index
  http: String,
) {
  override def equals(obj: Any): Boolean = obj match {
    case that: Validator =>
      this.publicKey.sameElements(that.publicKey) && this.http == that.http
    case _               => false
  }
}

object Validator {
  def toDb(id: Long, validator: Validator): ValidatorTable = ValidatorTable(id, validator.publicKey, validator.http)
  def fromDb(validator: ValidatorTable): Validator         = Validator(validator.publicKey, validator.http)
}

trait ValidatorDbApi[F[_]] {
  def insert(validator: Validator): F[Long]
  def update(id: Long, validator: Validator): F[Unit]

  def getById(id: Long): F[Option[Validator]]
  def getByPublicKey(publicKey: Array[Byte]): F[Option[Validator]]
}
