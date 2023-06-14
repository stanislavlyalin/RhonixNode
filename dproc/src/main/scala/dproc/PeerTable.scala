package dproc

import cats.effect.{Ref, Sync}
import cats.syntax.all._
import dproc.PeerTable._

final class PeerTable[F[_]: Sync, PId, P](private val stRef: Ref[F, ST[PId, P]]) {
  def addPeers(peers: Map[PId, P]): F[Unit] =
    stRef.update(state => ST(state.peers ++ peers))

  def removePeers(keys: Set[PId]): F[Unit] =
    stRef.update(state => ST(state.peers -- keys))

  def peers: F[Map[PId, P]] = stRef.get.map(_.peers)
}

object PeerTable {
  def apply[F[_]: Sync, PId, P]: F[PeerTable[F, PId, P]] = for {
    stRef <- Ref[F].of(emptyST[PId, P])
  } yield new PeerTable(stRef)

  def emptyST[PId, P]: ST[PId, P] = ST[PId, P](Map.empty[PId, P])

  final case class ST[PId, P](peers: Map[PId, P])
}
