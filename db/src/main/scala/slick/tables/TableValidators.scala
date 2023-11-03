package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableValidators.Validator

class TableValidators(tag: Tag) extends Table[Validator](tag, "validator") {
  def id: Rep[Long]            = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def pubKey: Rep[Array[Byte]] = column[Array[Byte]]("pub_key")

  def idx = index("idx_validator", pubKey, unique = true)

  def * : ProvenShape[Validator] = (id, pubKey).mapTo[Validator]
}

object TableValidators {
  final case class Validator(
    id: Long,           // primary key
    pubKey: Array[Byte],// validator public key
  )
}
