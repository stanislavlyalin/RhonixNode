package sdk.crypto

import scala.util.Try

trait Verifier {
  def verify(data: Array[Byte], signature: Sig, publicKey: PubKey): Try[Boolean]
}
