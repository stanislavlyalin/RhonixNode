package slick

import sdk.api.data.Validator
import slick.jdbc.JdbcProfile
final case class SlickQuery()(implicit val profile: JdbcProfile) {
  import profile.api.*

  def getValidatorById(id: Long) =
    validators.filter(_.id === id).result.headOption

  def insertValidator(validator: Validator) =
    (validators.map(r => r.publicKey) returning validators.map(_.id)) += validator.publicKey

  def updateValidator(id: Long, validator: Validator) =
    validators.filter(_.id === id).map(record => record.publicKey).update(validator.publicKey)

  def getValidatorByPublicKey(publicKey: Array[Byte]) =
    validators.filter(_.publicKey === publicKey).result.headOption
}
