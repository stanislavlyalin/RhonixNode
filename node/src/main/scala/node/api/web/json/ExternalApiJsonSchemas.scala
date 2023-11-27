package node.api.web.json

import sdk.api.data.*
import sdk.codecs.Base16
import sdk.syntax.all.*

/**
 * Json encoding for API types
 */
trait ExternalApiJsonSchemas extends JsonSchemasPretty {
  implicit val byteArrayJsonSchema: JsonSchema[Array[Byte]] =
    // TODO how to respond with 400 if decoding throws error?
    stringJsonSchema(None).xmap(s => Base16.decode(s).getUnsafe)(ba => Base16.encode(ba))

  implicit val balanceJson: JsonSchema[Balance]                          = genericRecord[Balance]
  implicit val deployJson: JsonSchema[Deploy]                            = genericRecord[Deploy]
  implicit val justificationJson: JsonSchema[Justification]              = genericRecord[Justification]
  implicit val bondJson: JsonSchema[Bond]                                = genericRecord[Bond]
  implicit val statusJson: JsonSchema[Status]                            = genericRecord[Status]
  implicit val blockJson: JsonSchema[Block]                              = genericRecord[Block]
  implicit val tokenTransferBJson: JsonSchema[TokenTransferRequest.Body] = genericRecord[TokenTransferRequest.Body]
  implicit val tokenTransferJson: JsonSchema[TokenTransferRequest]       = genericRecord[TokenTransferRequest]
}
