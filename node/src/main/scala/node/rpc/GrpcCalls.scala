package node.rpc

import cats.effect.kernel.Async
import cats.syntax.all.*
import node.comm.CommImpl.{blockApiDefinition, Block, BlockReceivedResponse}
import node.rpc.syntax.all.*
import slick.tables.TablePeers.Peer

object GrpcCalls {

  /**
   * Send block through grpc channel.
   */
  def sendBlock[F[_]: Async: GrpcChannelsManager](
    block: Block,
    to: Peer,
  ): F[BlockReceivedResponse] = {
    // For client calls it does not matter what is the message processing function.
    val apiDef = blockApiDefinition(_ => BlockReceivedResponse().pure[F])

    GrpcClient[F].callHost[Block, BlockReceivedResponse](GrpcMethod(apiDef), block, to.host, to.port)
  }
}
