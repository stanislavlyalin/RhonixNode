package secp256k1

import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.{
  ECDomainParameters,
  ECKeyGenerationParameters,
  ECPrivateKeyParameters,
  ECPublicKeyParameters,
}
import org.bouncycastle.math.ec.FixedPointUtil
import org.bitcoin.NativeSecp256k1
import sdk.crypto.*

import java.math.BigInteger
import java.security.SecureRandom
import scala.util.Try

/**
 * Key generation is done using BouncyCastle
 * Verification and signing using native library.
 */
object Secp256k1 {
  private val secKeyLength      = 32
  private val curveName: String = "secp256k1"

  // This is a copy of
  // https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/base/internal/ByteUtils.java#L83-L94
  private def bigIntegerToBytes(b: BigInteger, numBytes: Int): Array[Byte] = {
    assert(b.signum() >= 0, () -> s"b must be positive or zero: $b")
    assert(numBytes > 0, ()    -> s"numBytes must be positive: $numBytes")
    val src                    = b.toByteArray
    val dest                   = new Array[Byte](numBytes)
    val isFirstByteOnlyForSign = src(0) == 0
    val length                 = if (isFirstByteOnlyForSign) src.length - 1 else src.length
    assert(length <= numBytes, () -> s"The given number does not fit in $numBytes")
    val srcPos  = if (isFirstByteOnlyForSign) 1 else 0;
    val destPos = numBytes - length
    System.arraycopy(src, srcPos, dest, destPos, length)
    dest
  }

  private val CURVE_PARAMS: X9ECParameters = CustomNamedCurves.getByName(curveName)
  private val CURVE: ECDomainParameters    =
    new ECDomainParameters(CURVE_PARAMS.getCurve, CURVE_PARAMS.getG, CURVE_PARAMS.getN, CURVE_PARAMS.getH)
  private val secureRandom: SecureRandom   = SecureRandom.getInstanceStrong;
  private val generator                    = new ECKeyPairGenerator()
  private val keygenParams                 = new ECKeyGenerationParameters(CURVE, secureRandom)

  locally {
    val _ = FixedPointUtil.precompute(CURVE_PARAMS.getG)
    val _ = generator.init(keygenParams)
  }

  def apply: ECDSA = new ECDSA {
    override val dataSize: Int         = 32
    override val algorithmName: String = Secp256k1.curveName

    override def sign(data: Array[Byte], privateKey: SecKey): Try[Sig] = Try {
      assert(
        data.length == dataSize,
        s"Signed data has to be $dataSize bytes long for Secp256k1, but is ${data.length}",
      )
      assert(
        privateKey.value.length == secKeyLength,
        s"Private key has to be $secKeyLength bytes long for Secp256k1, but is ${privateKey.value.length}",
      )
      val sigArray = NativeSecp256k1.sign(data, privateKey.value)
      new Sig(sigArray)
    }

    override def verify(data: Array[Byte], signature: Sig, publicKey: PubKey): Try[Boolean] = Try {
      NativeSecp256k1.verify(data, signature.value, publicKey.value)
    }

    // This is a copied (along with initialization above in the Secp256k1 object scope) from
    // https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/crypto/ECKey.java#L173-L183
    override def newKeyPair: (SecKey, PubKey) = {
      val keypair                            = generator.generateKeyPair
      val privParams: ECPrivateKeyParameters = keypair.getPrivate.asInstanceOf[ECPrivateKeyParameters]
      val pubParams: ECPublicKeyParameters   = keypair.getPublic.asInstanceOf[ECPublicKeyParameters]

      new SecKey(bigIntegerToBytes(privParams.getD, secKeyLength)) ->
        new PubKey(pubParams.getQ.getEncoded(false))
    }
  }
}
