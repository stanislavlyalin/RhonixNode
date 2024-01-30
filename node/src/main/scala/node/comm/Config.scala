package node.comm

import node.comm.Config.PeerCfg
import sdk.reflect.Description

@Description("peerCfg")
final case class Config(
  @Description("Initial list of node peers")
  peers: List[PeerCfg] = List.empty[PeerCfg],
)

object Config {
  final case class PeerCfg(
    url: String,
    isSelf: Boolean,
    isValidator: Boolean,
  ) {
    override def toString = s"""{url: "$url", isSelf: $isSelf, isValidator: $isValidator}"""
  }

  // Default config has an example value as a basis when filling config manually
  val Default: Config = Config(List(PeerCfg("url", isSelf = false, isValidator = true)))
}
