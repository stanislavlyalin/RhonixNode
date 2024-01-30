package sdk.comm

/** Node's P2P communication module
 * Sends given message to all known peers except self via `broadcast` method
 * Receives messages from network with `receiver` stream */
trait Comm[F[_], M] {
  def broadcast(msg: M): F[Unit]
  def receiver: fs2.Stream[F, Unit]
}
