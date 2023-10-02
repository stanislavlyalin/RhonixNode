package slick

import sdk.api.data.Validator
import slick.jdbc.JdbcProfile

final case class SlickQuery()(implicit val profile: JdbcProfile) {
  import profile.api.*

  def getValidatorById(id: Long) =
    validators.filter(_.id === id).result.headOption

  def insertValidator(publicKey: Array[Byte]) =
    (validators.map(r => r.publicKey) returning validators.map(_.id)) += publicKey

  def updateValidator(validator: Validator) = validators.update(validator)

  def getValidatorByPublicKey(publicKey: Array[Byte]) =
    validators.filter(_.publicKey === publicKey).result.headOption
}
