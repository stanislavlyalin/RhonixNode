package sdk.history

import sdk.codecs.Codec
import sdk.primitive.ByteArray
import sdk.syntax.all.*

import scala.util.Try

/**
 * Represents a Blake2b 256 bit hash
 *
 * The default constructor is private to prevent construction of invalid hash, although constructions from
 * raw bytes are provided, see [[ByteArray32.deserialize]] functions.
 */
final case class ByteArray32 private (bytes: ByteArray) {

  require(
    bytes.length == ByteArray32.Length,
    s"Expected ${ByteArray32.Length} but got ${bytes.length}",
  )

  override def toString: String = s"Blake(${bytes.toHex})"
}

object ByteArray32 {

  // Ordering for hash uses underlying ordering of ByteArray
  implicit val ordering: Ordering[ByteArray32] = Ordering.by(_.bytes)

  val Length: Int = 32

  /**
   * Calculates Blake2b hash from bytes and creates [[ByteArray32]] instance.
   *
   * To create [[ByteArray32]] instance from raw bytes see [[ByteArray32.deserialize]] functions.
   *
   * @param bytes The bytes to hash
   */
  def apply(bytes: Array[Byte]): ByteArray32 =
    new ByteArray32(ByteArray(Blake2b.hash256(bytes)))

  // Serialization

  /** Deserialization from hash as raw bytes */
  def deserialize(bytes: ByteArray): Try[ByteArray32] = Try(new ByteArray32(bytes))

  /** Deserialization from hash as raw bytes */
  def deserialize(bytes: Array[Byte]): Try[ByteArray32] = deserialize(ByteArray(bytes))

  def codec: Codec[ByteArray32, ByteArray] = new Codec[ByteArray32, ByteArray] {
    override def encode(x: ByteArray32): Try[ByteArray] = Try(x.bytes)

    override def decode(x: ByteArray): Try[ByteArray32] = ByteArray32.deserialize(x)
  }
}
