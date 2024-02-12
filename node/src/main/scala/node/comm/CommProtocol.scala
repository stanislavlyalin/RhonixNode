package node.comm

import sdk.comm.Peer
import sdk.primitive.ByteArray

object CommProtocol {
  val serviceName = "rholab.foundation.CommService"

  trait CommMessage

  // Send a list of known peers to another peer
  final case class SendPeersRequest(peers: List[Peer]) extends CommMessage
  final case class SendPeersResponse()                 extends CommMessage

  // Check that the remote peer is alive
  val CheckPeerResponseOk = 0
  final case class CheckPeerRequest()           extends CommMessage
  final case class CheckPeerResponse(code: Int) extends CommMessage

  // Send message to remote peer
  final case class SendMessageRequest(msg: ByteArray) extends CommMessage
  final case class SendMessageResponse()              extends CommMessage
}
