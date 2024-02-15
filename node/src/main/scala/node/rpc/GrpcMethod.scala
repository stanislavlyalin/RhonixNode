package node.rpc

import io.grpc.MethodDescriptor
import sdk.api.ApiDefinition

import java.io.InputStream

/**
 * Constructor for Grpc method descriptor.
 * Used both for server and client.
 * */
object GrpcMethod {
  def apply[F[_], A, B](definition: ApiDefinition[F, A, B]): MethodDescriptor[A, B] =
    MethodDescriptor
      .newBuilder()
      .setType(MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(s"${definition.endpointName}")
      .setRequestMarshaller(
        new MethodDescriptor.Marshaller[A] {
          override def stream(obj: A): InputStream       = definition.serializeA(obj)
          override def parse(byteStream: InputStream): A = definition.parseA(byteStream)
        },
      )
      .build()
}
