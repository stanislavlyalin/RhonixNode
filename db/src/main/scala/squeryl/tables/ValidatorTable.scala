package squeryl.tables

import sdk.api.data.Validator

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
case class ValidatorTable(
  id: Long,
  publicKey: Array[Byte], // Unique index
//  http: String,
) extends DbTable

object ValidatorTable {
  def toDb(id: Long, validator: Validator): ValidatorTable = ValidatorTable(id, validator.publicKey)
  def fromDb(validator: ValidatorTable): Validator         = Validator(validator.publicKey)
}
