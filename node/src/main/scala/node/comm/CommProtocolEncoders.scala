package node.comm

import cats.Eval
import cats.syntax.all.*
import io.grpc.MethodDescriptor
import node.comm.CommProtocol.*
import sdk.comm.Peer

import java.io.InputStream

object CommProtocolEncoders {
  implicit val sendPeers: MethodDescriptor[SendPeersRequest, SendPeersResponse] = MethodDescriptor
    .newBuilder()
    .setType(MethodDescriptor.MethodType.UNARY)
    .setFullMethodName(s"$serviceName/SendPeers")
    .setRequestMarshaller(sendPeersRequestMarshal)
    .setResponseMarshaller(sendPeersResponseMarshal)
    .build()

  private lazy val sendPeersRequestMarshal = new MethodDescriptor.Marshaller[SendPeersRequest] {
    override def stream(obj: SendPeersRequest): InputStream       =
      Serialize.encode[SendPeersRequest](
        obj,
        (obj, writer) =>
          writer.write(obj.peers.size) *>
            writer.write(
              obj.peers,
              (p: Peer) => writer.write(p.url) *> writer.write(p.isSelf) *> writer.write(p.isValidator),
            ),
      )
    override def parse(byteStream: InputStream): SendPeersRequest =
      Serialize.decode[SendPeersRequest](
        byteStream,
        reader =>
          for {
            size  <- reader.readInt
            peers <- (0 until size).toList.traverse(_ =>
                       for {
                         url         <- reader.readString
                         isSelf      <- reader.readBool
                         isValidator <- reader.readBool
                       } yield Peer(url, isSelf, isValidator),
                     )

          } yield SendPeersRequest(peers),
      )
  }

  private lazy val sendPeersResponseMarshal = new MethodDescriptor.Marshaller[SendPeersResponse] {
    override def stream(obj: SendPeersResponse): InputStream       =
      Serialize.encode[SendPeersResponse](obj, (_, _) => Eval.always(()))
    override def parse(byteStream: InputStream): SendPeersResponse =
      Serialize.decode[SendPeersResponse](byteStream, _ => Eval.always(SendPeersResponse()))
  }

  implicit val checkPeer: MethodDescriptor[CheckPeerRequest, CheckPeerResponse] = MethodDescriptor
    .newBuilder()
    .setType(MethodDescriptor.MethodType.UNARY)
    .setFullMethodName(s"$serviceName/CheckPeer")
    .setRequestMarshaller(checkPeerRequestMarshal)
    .setResponseMarshaller(checkPeerResponseMarshal)
    .build()

  private lazy val checkPeerRequestMarshal = new MethodDescriptor.Marshaller[CheckPeerRequest] {
    override def stream(obj: CheckPeerRequest): InputStream       =
      Serialize.encode[CheckPeerRequest](obj, (_, _) => Eval.always(()))
    override def parse(byteStream: InputStream): CheckPeerRequest =
      Serialize.decode[CheckPeerRequest](byteStream, _ => Eval.always(CheckPeerRequest()))
  }

  private lazy val checkPeerResponseMarshal = new MethodDescriptor.Marshaller[CheckPeerResponse] {
    override def stream(obj: CheckPeerResponse): InputStream       =
      Serialize.encode[CheckPeerResponse](obj, (obj, writer) => writer.write(obj.code))
    override def parse(byteStream: InputStream): CheckPeerResponse =
      Serialize.decode[CheckPeerResponse](byteStream, reader => reader.readInt.map(CheckPeerResponse))
  }
}
