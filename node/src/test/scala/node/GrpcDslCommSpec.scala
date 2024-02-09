package node

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Sync}
import cats.syntax.all.*
import io.grpc.*
import node.comm.{GrpcClient, GrpcServer, Logger, Serialize}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.InputStream

class GrpcDslCommSpec extends AnyFlatSpec with Matchers {

  // Object sent over gRPC
  case class MyObj(text: String, num: Int)

  // Encoding functions using streams (using protobuf serializer)
  // NOTE: here low level validation can happen on the field level or even byte level,
  //       e.g. validating the message header like version or signature
  val myObjMarshal = new MethodDescriptor.Marshaller[MyObj] {
    override def stream(obj: MyObj): InputStream =
      Serialize.encode[MyObj](obj, (obj, writer) => writer.write(obj.text) *> writer.write(obj.num))

    override def parse(byteStream: InputStream): MyObj =
      Serialize.decode[MyObj](
        byteStream,
        reader =>
          for {
            text <- reader.readString
            num  <- reader.readInt
          } yield MyObj(text, num),
      )
  }

  /// Represents a method on the API (aka. method on gRPC service)
  val method: MethodDescriptor[MyObj, MyObj] = MethodDescriptor
    .newBuilder()
    .setType(MethodDescriptor.MethodType.UNARY)
    // Method name with the namespace prefix
    .setFullMethodName("coop.rchain.Service/MyMethod")
    // Encoder/decoder for input and output types
    .setRequestMarshaller(myObjMarshal)
    .setResponseMarshaller(myObjMarshal)
    .build()

  "grpc server & client" should "be defined with low level API directly" in {
    val serverHost = "localhost"
    val serverPort = 4321
    val srcMessage = MyObj("Hello from client!", 42)

    implicit val m: MethodDescriptor[MyObj, MyObj] = method
    implicit val l: Logger                         = Logger.console

    val serviceDef = ServerServiceDefinition
      .builder("coop.rchain.Service")
      .addMethod(method, GrpcServer.makeCallHandler { req: MyObj => MyObj(s"Server responds to: $req", 42) })
      .build()

    GrpcServer
      .apply[IO](serverPort, serviceDef)
      .use { _ =>
        GrpcClient.apply[IO](serverHost, serverPort).use { client =>
          for {
            resp <- client.send(srcMessage)
            _    <- Sync[IO].delay(Thread.sleep(250))
          } yield resp shouldBe MyObj(s"Server responds to: $srcMessage", 42)

        }
      }
      .unsafeRunSync()
  }
}
