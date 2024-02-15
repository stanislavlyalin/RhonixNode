package node.comm

import cats.Eval
import cats.effect.std.Queue
import cats.effect.{Async, Sync}
import cats.syntax.all.*
import node.rpc.{GrpcCalls, GrpcChannelsManager}
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

  /** Comm using grpc transport */
  def grpcComm[F[_]: Async](
    peerTable: PeerTable[F, String, Peer], // peers
    grpcStream: fs2.Stream[F, Block],      // stream of messages from grpc
  )(implicit channelsManager: GrpcChannelsManager[F]): F[Comm[F, Block, Block]] =
    Queue.unbounded[F, Block].map { inQ =>

      new Comm[F, Block, Block] {
        override def broadcast(msg: Block): F[Unit] = for {
          peers <- peerTable.all.map(_.values)
          _     <- peers.toList.traverse(peer => GrpcCalls.sendBlock[F](msg, peer))
        } yield ()

        override def receiver: fs2.Stream[F, Block] =
          // Yes this is effectively just a copy of a stream, but for now I don't have an idea how to make it better.
          fs2.Stream.fromQueueUnterminated(inQ) concurrently grpcStream.evalTap(inQ.offer)
      }
    }

  /** Comm using memory. */
  def inMemComm[F[_]: Async](
    outbound: List[Block => F[Unit]],
    inboundStream: fs2.Stream[F, Block],
  ): F[Comm[F, Block, Block]] =
    Queue.unbounded[F, Block].map { inQ =>

      new Comm[F, Block, Block] {
        override def broadcast(msg: Block): F[Unit] = outbound.traverse_(_.apply(msg))

        override def receiver: fs2.Stream[F, Block] =
          // Yes this is effectively just a copy of a stream, but for now I don't have an idea how to make it better.
          fs2.Stream.fromQueueUnterminated(inQ) concurrently inboundStream.evalTap(inQ.offer)
      }
    }
}
