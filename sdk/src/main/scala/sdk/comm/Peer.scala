package sdk.comm

final case class Peer(
  url: String,
  isSelf: Boolean,
  isValidator: Boolean,
) {
  override def equals(obj: Any): Boolean = obj match {
    case Peer(url, _, _) => this.url == url
    case _               => false
  }

  override def hashCode(): Int = url.hashCode
}
