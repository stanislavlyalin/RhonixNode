package node

import cats.Eval
import node.Serialization.balanceSerialize
import sdk.api.data.Balance
import sdk.codecs.Codec
import sdk.codecs.protobuf.{ProtoPrimitiveReader, ProtoPrimitiveWriter}
import sdk.primitive.ByteArray

import scala.util.Try

/** Implementation of the binary protocol. */
object Codecs {
  implicit val balanceCodec: Codec[Balance, ByteArray] = new Codec[Balance, ByteArray] {
    override def encode(x: Balance): Try[ByteArray] = Try {
      ProtoPrimitiveWriter.encodeWith(balanceSerialize[Eval].write(x)).map(ByteArray(_)).value
    }
    override def decode(x: ByteArray): Try[Balance] = Try {
      ProtoPrimitiveReader.decodeWith(x, balanceSerialize[Eval].read).value
    }
  }
}
