package sdk.crypto

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

import scala.util.Success

object ECDSASpec extends Matchers {
  // This spec is to be used for all ECDSA implementations
  def apply(ecdsa: ECDSA): Assertion = {
    val data   = Array.fill(ecdsa.dataSize)(1.toByte)
    val (s, p) = ecdsa.newKeyPair
    val sig    = ecdsa.sign(data, s).get
    // Successful verification
    ecdsa.verify(data, sig, p) shouldBe Success(true)
    // Unsuccessful verification
    ecdsa.verify(data, new Sig(sig.value.tail :+ 0.toByte), p) shouldBe Success(false)
  }
}
