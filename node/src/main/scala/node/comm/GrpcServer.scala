package node.comm

import cats.effect.{Resource, Sync}
import cats.syntax.all.*
import io.grpc.*
import io.grpc.netty.NettyServerBuilder

object GrpcServer {
  def apply[F[_]: Sync](port: Int, serviceDef: ServerServiceDefinition): Resource[F, Server] = Resource.make {
    Sync[F].delay {
      NettyServerBuilder
        .forPort(port)
        .addService(serviceDef)
        .build
        .start()
    }
  }(server => Sync[F].delay(server.shutdown()).void)

  def makeCallHandler[Req, Resp](responseF: Req => Resp)(implicit logger: Logger): ServerCallHandler[Req, Resp] =
    new ServerCallHandler[Req, Resp] {
      override def startCall(
        call: ServerCall[Req, Resp],
        headers: Metadata,
      ): ServerCall.Listener[Req] = {
        logger.info(s"SERVER_START_CALL: ${call.getMethodDescriptor}")
        logger.info(s"SERVER_START_CALL: $headers")

        // Number of messages to read next from the response (default is no read at all)
        call.request(1)

        new ServerCall.Listener[Req] {
          override def onMessage(message: Req): Unit = {
            logger.info(s"SERVER_ON_MESSAGE: $message")
            call.sendHeaders(headers)
            call.sendMessage(responseF(message))
            logger.info(s"SERVER_SENT_RESPOND_MSG")
            call.close(Status.OK, headers)
          }
          override def onHalfClose(): Unit           = logger.info(s"SERVER_ON_HALF_CLOSE")
          override def onCancel(): Unit              = logger.info(s"SERVER_ON_CANCEL")
          override def onComplete(): Unit            = logger.info(s"SERVER_ON_COMPLETE")
        }
      }
    }
}
