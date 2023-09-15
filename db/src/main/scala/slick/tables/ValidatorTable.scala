package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape

class ValidatorTableSlick(tag: Tag) extends Table[(Long, Array[Byte], String)](tag, "Validator") {
  def id: Rep[Long]               = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicKey: Rep[Array[Byte]] = column[Array[Byte]]("publicKey", O.Unique)
  def http: Rep[String]           = column[String]("http")

  def * : ProvenShape[(Long, Array[Byte], String)] = (id, publicKey, http)

  def idx = index("idx", publicKey, unique = true)
}

object ValidatorTableSlick {
  val validatorTableSlick = TableQuery[ValidatorTableSlick]
}
