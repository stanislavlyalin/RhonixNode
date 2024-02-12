package node.comm

import cats.data.OptionT
import cats.effect.{Async, Ref, Resource}
import cats.syntax.all.*
import io.grpc.*
import io.grpc.netty.NettyChannelBuilder
import sdk.syntax.all.*

import scala.concurrent.Promise

class GrpcClient[F[_]: Async] private (private val channelsRef: Ref[F, Map[(String, Int), ManagedChannel]]) {
  def send[Req, Resp](serverHost: String, serverPort: Int, msg: Req)(implicit
    method: MethodDescriptor[Req, Resp],
    logger: Logger,
  ): F[Resp] = for {
    channels <- channelsRef.updateAndGet { channels =>
                  if (channels.contains((serverHost, serverPort))) {
                    channels
                  } else {
                    val newChannel = NettyChannelBuilder
                      .forAddress(serverHost, serverPort)
                      .usePlaintext()
                      .build
                    channels + ((serverHost, serverPort) -> newChannel)
                  }
                }
    channel  <- OptionT
                  .fromOption(channels.get((serverHost, serverPort)))
                  .getOrRaise(new RuntimeException("gRPC channel for specified host has not been created"))
    response <- {
                  val call = channel.newCall[Req, Resp](method, CallOptions.DEFAULT)

                  val promise = Promise[Resp]()

                  val callListener = new ClientCall.Listener[Resp] {
                    override def onHeaders(headers: Metadata): Unit =
                      logger.info(s"CLIENT_ON_HEADERS: $headers")

                    override def onMessage(message: Resp): Unit = {
                      logger.info(s"CLIENT_ON_MESSAGE: $message")
                      val _ = promise.success(message)
                      super.onMessage(message)
                    }

                    override def onClose(status: Status, trailers: Metadata): Unit =
                      logger.info(s"CLIENT_ON_CLOSE: $status, $trailers")
                  }

                  call.start(callListener, new Metadata())

                  call.sendMessage(msg)
                  logger.info(s"CLIENT_SENT_MSG: $msg")

                  call.request(1)

                  call.halfClose()
                  logger.info(s"CLIENT_HALF_CLOSED")

                  promise.future
                }.asEffect
  } yield response

  def shutdown(): F[Unit] = channelsRef.get.map(_.foreachEntry { case (_, channel) => channel.shutdown() })
}

object GrpcClient {
  def apply[F[_]: Async]: Resource[F, GrpcClient[F]] =
    Resource.make(
      Ref.of(Map.empty[(String, Int), ManagedChannel]).map(ref => new GrpcClient[F](ref)),
    )(client => client.shutdown())
}
