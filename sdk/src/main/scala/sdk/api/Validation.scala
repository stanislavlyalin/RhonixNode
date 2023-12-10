package sdk.api

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.syntax.all.*
import sdk.api.data.TokenTransferRequest
import sdk.codecs.{Base16, Digest}
import sdk.crypto.{ECDSA, PubKey, Sig}
import sdk.syntax.all.sdkSyntaxTry

object Validation {
  type ValidationResult[+A] = ValidatedNel[ApiErr, A]

  // Just a random byte array indicating incorrect base16 string decoding from request's JSON data
  val InvalidBase16String: Array[Byte] =
    Base16.decode("ab2d294d56b4a4bfc124249fa6f27c012e9a04e6a30882443934ec697008aabe").getUnsafe

  def validateTokenTransferRequest(
    x: TokenTransferRequest,
  )(implicit
    signAlgs: Map[String, ECDSA],
    bodyDigest: Digest[TokenTransferRequest.Body],
  ): ValidationResult[TokenTransferRequest] =
    (validateBase16Encoding(x.pubKey, "pubKey") *>
      validateBase16Encoding(x.digest, "digest") *>
      validateBase16Encoding(x.signature, "signature") *>
      validateBodyDigest(x.body, x.digest) *>
      (for {
        // Checking length and signature only makes sense if sign algorithm has been validated
        signAlg <- validateSignatureAlg(x.signatureAlg).toEither
        _       <- validateSignatureLength(x.signature, signAlg).toEither
        _       <- validateSignature(x.signatureAlg, x.digest, x.signature, x.pubKey, signAlg).toEither
      } yield ()).toValidated *>
      validateTransferValue(x.body.value))
      .as(x)

  private def validateBase16Encoding(data: Array[Byte], fieldName: String): ValidationResult[Unit] =
    (!data.sameElements(InvalidBase16String))
      .guard[Option]
      .toValidNel(Base16DecodingFailed(fieldName))

  private def validateBodyDigest(body: TokenTransferRequest.Body, digest: Array[Byte])(implicit
    bodyDigest: Digest[TokenTransferRequest.Body],
  ): ValidationResult[Unit] =
    bodyDigest.digest(body).bytes.sameElements(digest).guard[Option].toValidNel(BodyDigestIsInvalid)

  private def validateSignatureAlg(signatureAlg: String)(implicit
    signAlgs: Map[String, ECDSA],
  ): ValidationResult[ECDSA] =
    signAlgs.get(signatureAlg).toValidNel(UnknownSignatureAlgorithm(signatureAlg))

  private def validateSignatureLength(sig: Array[Byte], signAlg: ECDSA): ValidationResult[Unit] =
    // (sig.length == signAlg.dataSize).guard[Option].toValidNel(SignatureLengthIsInvalid(sig.length, signAlg.dataSize))
    // TODO: Add correct implementation for signature length validation
    Valid(())

  private def validateSignature(
    signatureAlg: String,
    data: Array[Byte],
    sig: Array[Byte],
    pubKey: Array[Byte],
    signAlg: ECDSA,
  ): ValidationResult[Unit] =
    signAlg
      .verify(data, new Sig(sig), new PubKey(pubKey))
      .toOption
      .flatMap(_.guard[Option])
      .toValidNel(SignatureIsInvalid(sig, data, pubKey, signatureAlg))

  private def validateTransferValue(value: Long): ValidationResult[Unit] =
    (value > 0).guard[Option].toValidNel(TransferValueIsInvalid)
}
