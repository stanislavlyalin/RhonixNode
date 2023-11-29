package sdk.crypto

import scala.util.Try

trait Signer {
  def sign(data: Array[Byte], privateKey: SecKey): Try[Sig]
}
