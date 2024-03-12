package node.rpc

import cats.effect.{Async, Ref, Resource}
import cats.syntax.all.*
import io.grpc.*
import io.grpc.netty.NettyChannelBuilder

import java.net.InetSocketAddress

trait GrpcChannelsManager[F[_]] {
  def get(socketAddress: InetSocketAddress): F[ManagedChannel]
}

object GrpcChannelsManager {
  def apply[F[_]: Async]: Resource[F, GrpcChannelsManager[F]] = Resource
    .make(Ref.of(Map.empty[InetSocketAddress, ManagedChannel]))(_.get.map(_.values.foreach(_.shutdown())))
    .map { channelsRef =>
      new GrpcChannelsManager[F] {
        def get(socketAddress: InetSocketAddress): F[ManagedChannel] = channelsRef.modify { st =>
          st.get(socketAddress) match {
            case Some(channel) => (st, channel)
            case None          =>
              val newChannel = NettyChannelBuilder.forAddress(socketAddress).usePlaintext().build
              val newSt      = st + (socketAddress -> newChannel)
              newSt -> newChannel
          }
        }
      }
    }
}
