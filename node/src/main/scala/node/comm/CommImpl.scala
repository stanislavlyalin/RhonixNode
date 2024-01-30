package node.comm

import cats.effect.Sync
import cats.syntax.all.*
import dproc.DProc
import sdk.comm.{Comm, Peer}

final class CommImpl[F[_]: Sync, M, T](
  private val peerTable: PeerTable[F, String, Peer],

  /** In the future we will only need the peer URL to send a message,
   * but for now in the simulator we need an instance of the process */
  private val peerProc: Map[String, DProc[F, M, T]],
) extends Comm[F, M] {

  override def broadcast(msg: M): F[Unit] = for {
    peers <- peerTable.all.map(_.values.toSeq)
    _     <- peers.filterNot(_.isSelf).traverse(peer => peerProc.get(peer.url).map(_.acceptMsg(msg)).getOrElse(().pure))
  } yield ()

  // NOTE: Not used yet, so empty
  override def receiver: fs2.Stream[F, Unit] = fs2.Stream.empty
}
