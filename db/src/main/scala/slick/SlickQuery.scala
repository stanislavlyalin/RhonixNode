package slick

import slick.dbio.Effect.{Read, Write}
import slick.jdbc.JdbcProfile
import slick.sql.SqlAction
import slick.tables.TableValidators.Validator

final case class SlickQuery()(implicit val profile: JdbcProfile) {
  import profile.api.*

  def getValidatorById(id: Long): SqlAction[Option[Validator], NoStream, Read] =
    qValidators.filter(_.id === id).result.headOption

  def getValidatorByPubKey(publicKey: Array[Byte]): SqlAction[Option[Validator], NoStream, Read] =
    qValidators.filter(_.pubKey === publicKey).result.headOption

  def insertValidator(publicKey: Array[Byte]): SqlAction[Long, NoStream, Write] =
    (qValidators.map(r => r.pubKey) returning qValidators.map(_.id)) += publicKey

  def updateValidator(validator: Validator): SqlAction[Int, NoStream, Write] =
    qValidators.update(validator)

  def storeValue(key: String, value: String): SqlAction[Int, NoStream, Effect.Write] =
    qConfigs.insertOrUpdate((key, value))

  def loadValue(key: String): SqlAction[Option[String], NoStream, Effect.Read] =
    qConfigs.filter(_.name === key).map(_.value).result.headOption
}
