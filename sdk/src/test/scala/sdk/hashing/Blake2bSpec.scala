package sdk.hashing

import org.scalacheck.ScalacheckShapeless.arbitrarySingletonType
import org.scalacheck.{Arbitrary, Gen, Prop}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.Checkers
import sdk.history.ByteArray32
import sdk.history.ByteArray32.codec
import sdk.primitive.ByteArray
import sdk.syntax.all.*

import java.util

class Blake2bSpec extends AnyFlatSpec with Checkers {

  "The bytes of a Blake2b256 hash" should "be the same as if it was created directly" in {
    val propCreate: Prop = Prop.forAll { (bytes: Array[Byte]) =>
      util.Arrays.equals(Blake2b.hash256(bytes), ByteArray32(bytes).bytes.bytes)
    }
    check(propCreate)
  }

  "A Blake2b256 hash" should "be the same when round-tripped with scodec" in {
    val propRoundTripCodec: Prop = Prop.forAll { (bytes: Array[Byte]) =>
      val hash    = ByteArray32(bytes)
      val encoded = codec.encode(hash)
      val decoded = codec.decode(encoded.getUnsafe)
      hash.bytes == decoded.getUnsafe.bytes
    }
    check(propRoundTripCodec)
  }

  "Method fromByteArray()" should "create Blake2b256Hash from 32 byte array" in {
    val byteArrayGen: Gen[Array[Byte]] = Gen.listOfN(ByteArray32.Length, Arbitrary.arbitrary[Byte]).map(_.toArray)
    val propFromByteArray: Prop        = Prop.forAll(byteArrayGen) { (bytes: Array[Byte]) =>
      val hashTry = ByteArray32.deserialize(bytes)
      hashTry.isSuccess && hashTry.getUnsafe.bytes == ByteArray(bytes)
    }
    check(propFromByteArray)
  }

  "Object creation when bytes array is not of the correct length" should "fail" in {
    val byteArrayGen: Gen[Array[Byte]] = Gen.choose(0, 100).filterNot(_ == ByteArray32.Length).flatMap { size =>
      Gen.listOfN(size, Arbitrary.arbitrary[Byte]).map(_.toArray)
    }
    val propFromByteArray: Prop        = Prop.forAll(byteArrayGen) { (bytes: Array[Byte]) =>
      ByteArray32.deserialize(bytes).isFailure
    }
    check(propFromByteArray)
  }
}
