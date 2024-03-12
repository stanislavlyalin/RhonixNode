package node.rpc

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Resource, Sync}
import cats.syntax.all.*
import io.grpc.*
import io.grpc.netty.NettyServerBuilder
import node.comm.CommImpl.{BlockHash, BlockHashResponse}
import sdk.comm.CommProtocol
import sdk.log.Logger

object GrpcServer {
  def apply[F[_]: Async](
    port: Int,
    blockExchangeProtocol: CommProtocol[F, BlockHash, BlockHashResponse],
  ): Resource[F, Server] =
    Dispatcher.sequential[F].flatMap { dispatcher =>
      val serviceDefinition: ServerServiceDefinition = ServerServiceDefinition
        .builder(sdk.api.RootPathString)
        .addMethod(
          GrpcMethod(blockExchangeProtocol),
          mkMethodHandler(blockExchangeProtocol, dispatcher),
        )
        .build()

      Resource.make(
        Sync[F].delay {
          NettyServerBuilder
            .forPort(port)
            .addService(serviceDefinition)
            .build
            .start()
        },
      )(server => Sync[F].delay(server.shutdown()).void)
    }

    }
  }
}
