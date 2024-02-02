package node.comm

import io.grpc.MethodDescriptor
import node.comm.CommProtocol.*

import java.io.InputStream

object CommProtocolEncoders {

  // TODO: Consider to use PrimitiveWriter & PrimitiveReader

  implicit val sendPeers: MethodDescriptor[SendPeersRequest, SendPeersResponse] = MethodDescriptor
    .newBuilder()
    .setType(MethodDescriptor.MethodType.UNARY)
    .setFullMethodName(s"$serviceName/SendPeers")
    .setRequestMarshaller(sendPeersRequestMarshal)
    .setResponseMarshaller(sendPeersResponseMarshal)
    .build()

  private lazy val sendPeersRequestMarshal = new MethodDescriptor.Marshaller[SendPeersRequest] {
    override def stream(obj: SendPeersRequest): InputStream       = ???
    override def parse(byteStream: InputStream): SendPeersRequest = ???
  }

  private lazy val sendPeersResponseMarshal = new MethodDescriptor.Marshaller[SendPeersResponse] {
    override def stream(obj: SendPeersResponse): InputStream       = ???
    override def parse(byteStream: InputStream): SendPeersResponse = ???
  }

  implicit val checkPeer: MethodDescriptor[CheckPeerRequest, CheckPeerResponse] = MethodDescriptor
    .newBuilder()
    .setType(MethodDescriptor.MethodType.UNARY)
    .setFullMethodName(s"$serviceName/CheckPeer")
    .setRequestMarshaller(checkPeerRequestMarshal)
    .setResponseMarshaller(checkPeerResponseMarshal)
    .build()

  private lazy val checkPeerRequestMarshal = new MethodDescriptor.Marshaller[CheckPeerRequest] {
    override def stream(obj: CheckPeerRequest): InputStream       = ???
    override def parse(byteStream: InputStream): CheckPeerRequest = ???
  }

  private lazy val checkPeerResponseMarshal = new MethodDescriptor.Marshaller[CheckPeerResponse] {
    override def stream(obj: CheckPeerResponse): InputStream       = ???
    override def parse(byteStream: InputStream): CheckPeerResponse = ???
  }
}
