package node.comm

import cats.effect.std.{Dispatcher, Queue}
import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.*
import io.grpc.{Server, ServerServiceDefinition}
import node.comm.CommProtocol.*
import node.comm.CommProtocolEncoders.*
import sdk.comm.{Comm, Peer}
import sdk.primitive.ByteArray

final class CommImpl[F[_]: Sync, M] private (
  private val peerTable: PeerTable[F, String, Peer],
  private val receiveQueue: Queue[F, CommMessage],
  private val client: GrpcClient[F],
)(implicit val logger: Logger, msgEncoder: M => ByteArray)
    extends Comm[F, M, CommMessage] {

  override def broadcast(msg: M): F[Unit] = for {
    peers <- peerTable.all.map(_.values.toSeq)
    _     <-
      peers
        .filterNot(_.isSelf)
        .traverse(peer =>
          client.send[SendMessageRequest, SendMessageResponse](
            peer.host,
            peer.port,
            SendMessageRequest(msgEncoder(msg)),
          ),
        )
  } yield ()

  override def receiver: fs2.Stream[F, CommMessage] = fs2.Stream.fromQueueUnterminated(receiveQueue)
}

object CommImpl {
  def apply[F[_]: Async, M](gRpcPort: Int, peerTable: PeerTable[F, String, Peer])(implicit
    logger: Logger,
    msgEncoder: M => ByteArray,
  ): F[CommImpl[F, M]] =
    for {
      receiveQueue <- Queue.unbounded[F, CommMessage]
      comm         <- Dispatcher.sequential[F].use { dispatcher =>
                        createServer(gRpcPort, receiveQueue, dispatcher).use { _ =>
                          GrpcClient[F].use { client =>
                            Sync[F].delay {
                              new CommImpl[F, M](peerTable, receiveQueue, client)
                            }
                          }
                        }
                      }
    } yield comm

  private def createServer[F[_]: Async](
    gRpcPort: Int,
    receiveQueue: Queue[F, CommMessage],
    dispatcher: Dispatcher[F],
  )(implicit logger: Logger): Resource[F, Server] = {
    val serviceDef =
      ServerServiceDefinition
        .builder(CommProtocol.serviceName)
        .addMethod(
          CommProtocolEncoders.sendPeers,
          GrpcServer.makeCallHandler {
            request: SendPeersRequest =>
              dispatcher.unsafeRunSync(receiveQueue.offer(request))
              SendPeersResponse()
          },
        )
        .addMethod(
          CommProtocolEncoders.checkPeer,
          GrpcServer.makeCallHandler {
            request: CheckPeerRequest =>
              dispatcher.unsafeRunSync(receiveQueue.offer(request))
              CheckPeerResponse(CheckPeerResponseOk)
          },
        )
        .addMethod(
          CommProtocolEncoders.sendMessage,
          GrpcServer.makeCallHandler {
            request: SendMessageRequest =>
              dispatcher.unsafeRunSync(receiveQueue.offer(request))
              SendMessageResponse()
          },
        )
        .build()

    GrpcServer(gRpcPort, serviceDef)
  }
}
