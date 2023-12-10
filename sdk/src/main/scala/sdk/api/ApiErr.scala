package sdk.api

import cats.Show
import sdk.syntax.all.*

sealed trait ApiErr

// Base16 decoding failed
final case class Base16DecodingFailed(s: String) extends ApiErr

// Body digest is invalid
case object BodyDigestIsInvalid extends ApiErr

// Unknown signature algorithm
final case class UnknownSignatureAlgorithm(signatureAlg: String) extends ApiErr

// Signature length is incorrect
final case class SignatureLengthIsInvalid(actualLength: Int, requiredLength: Int) extends ApiErr

// Signature of the data is invalid
final case class SignatureIsInvalid(sig: Array[Byte], data: Array[Byte], pubKey: Array[Byte], sigAlg: String)
    extends ApiErr

// Transfer value is invalid
case object TransferValueIsInvalid extends ApiErr

object ApiErr {
  implicit val apiErrShow: Show[ApiErr] = new Show[ApiErr] {
    override def show(t: ApiErr): String = t match {
      case Base16DecodingFailed(s)                                =>
        s"Error decoding $s with Base16 algorithm"
      case BodyDigestIsInvalid                                    =>
        "Body digest does not match the request body"
      case UnknownSignatureAlgorithm(signatureAlg: String)        =>
        s"Signature algorithm $signatureAlg is not supported"
      case SignatureLengthIsInvalid(actualLength, requiredLength) =>
        s"Signature length is incorrect: $actualLength bytes instead of the required $requiredLength bytes"
      case SignatureIsInvalid(sig, digest, pubKey, sigAlg)        =>
        s"Invalid signature ${sig.toHex} for ${digest.toHex} given pubKey: ${pubKey.toHex} and sigAlg: $sigAlg"
      case TransferValueIsInvalid                                 =>
        "Transfer value should be positive number"
      case _                                                      =>
        "Request processing error"
    }
  }
}
