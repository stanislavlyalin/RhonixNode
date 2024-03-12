package node.rpc

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Resource, Sync}
import cats.syntax.all.*
import dproc.data.Block
import io.grpc.*
import io.grpc.netty.NettyServerBuilder
import node.Serialization.*
import sdk.api.{BlockEndpoint, BlockHashEndpoint, LatestBlocksEndpoint}
import sdk.data.BalancesDeploy
import sdk.log.Logger.*
import sdk.primitive.ByteArray

import java.net.InetSocketAddress

object GrpcServer {
  def apply[F[_]: Async](
    port: Int,
    hashRcvF: (ByteArray, InetSocketAddress) => F[Boolean],
    blockResolve: ByteArray => F[Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]],
    latestBlocks: Unit => F[Seq[ByteArray]],
  ): Resource[F, Server] =
    Dispatcher.sequential[F].flatMap { dispatcher =>
      val serviceDefinition: ServerServiceDefinition = ServerServiceDefinition
        .builder(sdk.api.RootPathString)
        .addMethod(
          GrpcMethod[(ByteArray, InetSocketAddress), Boolean](BlockHashEndpoint),
          GrpcMethodHandler(hashRcvF.tupled, dispatcher),
        )
        .addMethod(
          GrpcMethod[ByteArray, Option[Block.WithId[ByteArray, ByteArray, BalancesDeploy]]](BlockEndpoint),
          GrpcMethodHandler(blockResolve, dispatcher),
        )
        .addMethod(
          GrpcMethod[Unit, Seq[ByteArray]](LatestBlocksEndpoint),
          GrpcMethodHandler(latestBlocks, dispatcher),
        )
        .build()

      Resource.make(
        Sync[F].delay {
          NettyServerBuilder
            .forPort(port)
            .addService(serviceDefinition)
            .build
            .start()
        } <* logInfoF(s"RPC server started on port $port."),
      )(server => Sync[F].delay(server.shutdown()) *> logInfoF(s"RPC server stopped."))
    }
}
