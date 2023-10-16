package slick

import slick.jdbc.JdbcProfile
import slick.sql.SqlAction
import slick.tables.TableValidator.Validator

final case class SlickQuery()(implicit val profile: JdbcProfile) {
  import profile.api.*

  def getValidatorById(id: Long) =
    qValidator.filter(_.id === id).result.headOption

  def getValidatorByPubKey(publicKey: Array[Byte]) =
    qValidator.filter(_.pubKey === publicKey).result.headOption

  def insertValidator(publicKey: Array[Byte]) =
    (qValidator.map(r => r.pubKey) returning qValidator.map(_.id)) += publicKey

  def updateValidator(validator: Validator) = qValidator.update(validator)

  def storeValue(key: String, value: String): SqlAction[Int, NoStream, Effect.Write] =
    config.insertOrUpdate((key, value))

  def loadValue(key: String): SqlAction[Option[String], NoStream, Effect.Read] =
    config.filter(_.name === key).map(_.value).result.headOption
}
