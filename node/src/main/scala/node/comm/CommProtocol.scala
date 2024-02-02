package node.comm

import sdk.comm.Peer

object CommProtocol {
  val serviceName = "rholab.foundation.CommService"

  // Send a list of known peers to another peer
  final case class SendPeersRequest(peers: List[Peer])
  final case class SendPeersResponse()

  // Check that the remote peer is alive
  val CheckPeerResponseOk = 0
  final case class CheckPeerRequest()
  final case class CheckPeerResponse(code: Int)
}
