package node.comm

import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.*
import io.grpc.*
import io.grpc.netty.NettyChannelBuilder
import sdk.syntax.all.*

import scala.concurrent.Promise

class GrpcClient[F[_]: Async] private (private val channel: ManagedChannel) {
  def send[Req, Resp](msg: Req)(implicit method: MethodDescriptor[Req, Resp], logger: Logger): F[Resp] = for {
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

  def shutdown: ManagedChannel = channel.shutdown()
}

object GrpcClient {
  def apply[F[_]: Async](serverHost: String, serverPort: Int): Resource[F, GrpcClient[F]] =
    Resource.make {
      Sync[F].delay {
        val channel = NettyChannelBuilder
          .forAddress(serverHost, serverPort)
          .usePlaintext()
          .build

        new GrpcClient[F](channel)
      }
    }(client => Sync[F].delay(client.shutdown).void)
}
