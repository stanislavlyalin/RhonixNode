package sdk.api

import cats.data.ValidatedNel
import cats.syntax.all.*
import sdk.api.data.TokenTransferRequest

object Validation {
  // TODO validate class content, ptal https://typelevel.org/cats/datatypes/validated.html
  def validateTokenTransferRequest(x: TokenTransferRequest): ValidatedNel[ApiErr, TokenTransferRequest] =
    x.signature.nonEmpty
      .guard[Option]
      .as(x)
      .toValidNel(SignatureIsInvalid(x.signature, x.digest, x.pubKey, x.signatureAlg))
}
