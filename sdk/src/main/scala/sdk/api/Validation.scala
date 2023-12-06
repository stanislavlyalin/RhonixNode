package sdk.api

import cats.data.ValidatedNel
import cats.syntax.all.*
import sdk.api.data.TokenTransferRequest
import sdk.codecs.{Base16, Digest}
import sdk.crypto.{ECDSA, PubKey, Sig}
import sdk.syntax.all.sdkSyntaxTry

object Validation {
  private type ValidationResult[+A] = ValidatedNel[ApiErr, A]

  // Just a random byte array indicating incorrect base16 string decoding from request's JSON data
  val InvalidBase16String: Array[Byte] =
    Base16.decode("ab2d294d56b4a4bfc124249fa6f27c012e9a04e6a30882443934ec697008aabe").getUnsafe

  def validateTokenTransferRequest(
    x: TokenTransferRequest,
  )(implicit ecdsa: ECDSA, bodyDigest: Digest[TokenTransferRequest.Body]): ValidationResult[TokenTransferRequest] = (
    validateBase16Encoding(x.pubKey, "pubKey"),
    validateBase16Encoding(x.digest, "digest"),
    validateBase16Encoding(x.signature, "signature"),
    validateBodyDigest(x.body, x.digest),
    validateSignature(x.signatureAlg, x.digest, x.signature, x.pubKey),
    validateTransferValue(x.body.value),
  ).mapN { case (_, _, _, _, _, _) => x }

  private def validateBase16Encoding(data: Array[Byte], fieldName: String): ValidationResult[Array[Byte]] =
    (!data.sameElements(InvalidBase16String))
      .guard[Option]
      .as(data)
      .toValidNel(Base16DecodingFailed(fieldName))

  private def validateBodyDigest(body: TokenTransferRequest.Body, digest: Array[Byte])(implicit
    bodyDigest: Digest[TokenTransferRequest.Body],
  ): ValidationResult[Array[Byte]] =
    bodyDigest.digest(body).bytes.sameElements(digest).guard[Option].as(digest).toValidNel(BodyDigestIsInvalid)

  private def validateSignature(
    signatureAlg: String,
    data: Array[Byte],
    sig: Array[Byte],
    pubKey: Array[Byte],
  )(implicit ecdsa: ECDSA): ValidationResult[Unit] = {
    val signLenCorrectOpt     = (sig.length == ecdsa.dataSize).guard[Option]
    val signAlgNameCorrectOpt = (signatureAlg == ecdsa.algorithmName).guard[Option]
    val signatureCorrectOpt   = ecdsa.verify(data, new Sig(sig), new PubKey(pubKey)).toOption.flatMap(_.guard[Option])

    ((signLenCorrectOpt ++ signAlgNameCorrectOpt ++ signatureCorrectOpt).toSeq.length == 3)
      .guard[Option]
      .toValidNel(SignatureIsInvalid(sig, data, pubKey, signatureAlg))
  }

  private def validateTransferValue(value: Long): ValidationResult[Long] =
    (value > 0).guard[Option].as(value).toValidNel(TransferValueIsInvalid)
}
