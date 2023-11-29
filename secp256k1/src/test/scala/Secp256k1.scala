import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sdk.crypto.Sig
import secp256k1.Secp256k1

import scala.util.Success

class Secp256k1 extends AnyFlatSpec with Matchers {
  "Secp256k1" should "sign and verify" in {
    val data   = Array.fill(32)(1.toByte)
    val (s, p) = Secp256k1.newKeyPair
    val sig    = Secp256k1.sign(data, s).get
    // Successful verification
    Secp256k1.verify(data, sig, p) shouldBe Success(true)
    // Unsuccessful verification
    Secp256k1.verify(data, new Sig(sig.value.tail :+ 0.toByte), p) shouldBe Success(false)
  }
}
