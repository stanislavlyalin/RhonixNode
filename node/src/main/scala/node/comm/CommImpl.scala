package node.comm

import cats.Eval
import cats.effect.std.Queue
import cats.effect.{Async, Sync}
import cats.syntax.all.*
import sdk.api.ApiDefinition
import sdk.comm.{Comm, Peer}
import sdk.primitive.ByteArray

import java.io.InputStream

object CommImpl {

  trait CommMessage

  // Send message to remote peer
  final case class Block(msg: ByteArray)   extends CommMessage
  final case class BlockReceivedResponse() extends CommMessage

  /** Protocol for blocks exchange. TODO move to sdk */
  def blockApiDefinition[F[_]: Sync](
    f: Block => F[BlockReceivedResponse],
  ): ApiDefinition[F, Block, BlockReceivedResponse] =
    ApiDefinition[F, Block, BlockReceivedResponse](
      "rholab.foundation.CommService",
      (obj: Block) => Serialize.encode[Block](obj, (obj, writer) => writer.write(obj.msg.bytes)),
      (byteStream: InputStream) =>
        Serialize.decode[Block](
          byteStream,
          reader => reader.readBytes.map(bytes => Block(ByteArray(bytes))),
        ),
      f,
      (obj: BlockReceivedResponse) => Serialize.encode[BlockReceivedResponse](obj, (_, _) => Eval.always(())),
      (byteStream: InputStream) =>
        Serialize.decode[BlockReceivedResponse](byteStream, _ => Eval.always(BlockReceivedResponse())),
    )

  /** Implementation of Comm using grpc with message buffering. */
  def grpcBuffered[F[_]: Async](peerTable: PeerTable[F, String, Peer]): F[(Comm[F, Block, Block], Block => F[BlockReceivedResponse])] =
    (Queue.unbounded[F, Block],Queue.unbounded[F, Block]).mapN { case (iQ, oQ) =>
      val inbound = iQ.tryOffer(_).map(_ => BlockReceivedResponse())
      val outbound = oQ.tryOffer(_)

      val comm = new Comm[F, Block, Block] {
        override def broadcast(msg: Block): F[Unit] = peerTable.all.map(_.values.toSeq).flatMap { peers =>

          peers
            .filterNot(_.isSelf)
            .traverse_(peer => , )
        }
        override def receiver: fs2.Stream[F, Block] = fs2.Stream.fromQueueUnterminated(queue)
      }
      comm -> processInput
    }

}
