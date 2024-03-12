package node.rpc.syntax

import cats.data.Kleisli
import cats.effect.Async
import cats.syntax.all.*
import cats.{Eval, Monad}
import dproc.data.Block
import io.grpc.MethodDescriptor
import node.comm.PeerTable
import node.rpc.{GrpcChannelsManager, GrpcClient, GrpcMethod}
import sdk.api.{BlockEndpoint, BlockHashEndpoint, LatestBlocksEndpoint}
import sdk.codecs.Serialize
import sdk.comm.Peer
import sdk.data.BalancesDeploy
import sdk.primitive.ByteArray

import java.net.InetSocketAddress

trait GrpcClientSyntax {
  implicit def grpcClientSyntax[F[_]](client: GrpcClient[F]): GrpcClientOps[F] = new GrpcClientOps[F](client)
}

final class GrpcClientOps[F[_]](private val client: GrpcClient[F]) extends AnyVal {

  /** Call specifying endpoint with host and port. */
  private def callMethod[Req, Resp](
    method: MethodDescriptor[Req, Resp],
    msg: Req,
    InetSocketAddress: InetSocketAddress,
  )(implicit
    channelsManager: GrpcChannelsManager[F],
    monadF: Monad[F],
  ): F[Resp] = channelsManager.get(InetSocketAddress).flatMap(client.call(method, msg, _))

  private def callEndpoint[Req, Resp](endpoint: String, msg: Req, InetSocketAddress: InetSocketAddress)(implicit
    F: Monad[F],
    gcm: GrpcChannelsManager[F],
    sA: Serialize[Eval, Req],
    sB: Serialize[Eval, Resp],
  ): F[Resp] = callMethod[Req, Resp](GrpcMethod[Req, Resp](endpoint), msg, InetSocketAddress)

  def resolveBlock(InetSocketAddress: InetSocketAddress)(implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
  ): Kleisli[F, ByteArray, Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]] = {
    import node.Serialization.*
    Kleisli {
      hash: ByteArray =>
        callEndpoint[ByteArray, Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]](
          BlockEndpoint,
          hash,
          InetSocketAddress,
        )
    }
  }

  def getLatestBlocks(InetSocketAddress: InetSocketAddress)(implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
  ): F[Seq[ByteArray]] = {
    import node.Serialization.*
    callEndpoint[Unit, Seq[ByteArray]](LatestBlocksEndpoint, (), InetSocketAddress)
  }

  def reportBlockHash(hash: ByteArray, suggestedResolver: InetSocketAddress, InetSocketAddress: InetSocketAddress)(
    implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
  ): F[Boolean] = {
    import node.Serialization.*
    callEndpoint[(ByteArray, InetSocketAddress), Boolean](
      BlockHashEndpoint,
      (hash, suggestedResolver),
      InetSocketAddress,
    )
  }

  def broadcastBlockHash(hash: ByteArray, resolver: InetSocketAddress)(implicit
    F: Async[F],
    c: GrpcChannelsManager[F],
    peerManager: PeerTable[F, (String, Int), Peer],
  ): F[Unit] =
    for {
      peers <- peerManager.all.map(_.values.filterNot(_.isSelf).toList)
      _     <- peers.traverse { peer =>
                 val socket = new InetSocketAddress(peer.host, peer.port)
                 reportBlockHash(hash, resolver, socket)
               }
    } yield ()
}
