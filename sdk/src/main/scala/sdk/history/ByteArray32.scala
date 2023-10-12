package sdk.history

import sdk.codecs.Codec
import sdk.primitive.ByteArray
import sdk.syntax.all.*

import scala.util.Try

final case class ByteArray32 private (bytes: ByteArray) {

  require(
    bytes.length == ByteArray32.Length,
    s"Expected ${ByteArray32.Length} but got ${bytes.length}",
  )

  override def toString: String = s"Blake(${bytes.toHex})"
}

object ByteArray32 {

  implicit val ordering: Ordering[ByteArray32] = Ordering.by(_.bytes)

  val Length: Int = 32

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
