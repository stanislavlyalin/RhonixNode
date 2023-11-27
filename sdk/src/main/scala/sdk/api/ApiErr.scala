package sdk.api

import cats.Show
import sdk.syntax.all.*

sealed trait ApiErr

// Signature of the data is invalid
final case class SignatureIsInvalid(sig: Array[Byte], data: Array[Byte], pubKey: Array[Byte], sigAlg: String)
    extends ApiErr

object ApiErr {
  implicit val apiErrShow: Show[ApiErr] = new Show[ApiErr] {
    override def show(t: ApiErr): String = t match {
      case SignatureIsInvalid(sig, digest, pubKey, sigAlg) =>
        s"Invalid signature ${sig.toHex} for ${digest.toHex} given pubKey: ${pubKey.toHex} and sigAlg: $sigAlg"
    }
  }
}
