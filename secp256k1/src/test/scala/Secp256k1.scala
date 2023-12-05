import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sdk.crypto.ECDSASpec
import secp256k1.Secp256k1

class Secp256k1 extends AnyFlatSpec with Matchers {
  "Secp256k1" should "pass ECDSASpec" in {
    ECDSASpec(Secp256k1.apply)
  }
}
