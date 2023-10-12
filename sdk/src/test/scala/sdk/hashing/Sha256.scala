package sdk.hashing

import sdk.history.ByteArray32
import sdk.syntax.all.sdkSyntaxTry

import java.security.MessageDigest

object Sha256 {

  implicit def hash(input: Array[Byte]): ByteArray32 =
    ByteArray32.deserialize(MessageDigest.getInstance("SHA-256").digest(input)).getUnsafe
}
