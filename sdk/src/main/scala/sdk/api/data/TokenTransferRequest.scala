package sdk.api.data

import sdk.api.data.TokenTransferRequest.Body

final case class TokenTransferRequest(
  pubKey: Array[Byte],    // public key of the sender
  digest: Array[Byte],    // body digest
  signature: Array[Byte], // signature of a digest
  signatureAlg: String,   // "secp256k1"
  body: Body,
)

object TokenTransferRequest {
  final case class Body(
    from: Array[Byte],
    to: Array[Byte],
    tokenId: Long,
    value: Long,
    vafn: Long,
  )
}
