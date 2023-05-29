package dproc

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

trait FContext {
  type F[A] = IO[A]
}

class PeerTableSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with FContext {
  final case class Endpoint(host: String, tcpPort: Int = 0, udpPort: Int = 0)
  final case class PeerNode(id: Int, endpoint: Endpoint)

  "self initial state should be empty" in {
    PeerTable[F, Int, PeerNode].flatMap(_.peers.map(_ shouldBe empty))
  }

  "addPeers should add a node to the table" in {
    for {
      peerTable <- PeerTable[F, Int, PeerNode]
      newPeer = PeerNode(1, Endpoint("localhost"))
      _ <- peerTable.addPeers(Map(newPeer.id -> newPeer))
      peers <- peerTable.peers
    } yield peers shouldBe Map(newPeer.id -> newPeer)
  }

  "addPeers should only add unique nodes to the table" in {
    for {
      peerTable <- PeerTable[F, Int, PeerNode]
      newPeer = PeerNode(1, Endpoint("localhost"))
      _ <- peerTable.addPeers(Map(newPeer.id -> newPeer))
      _ <- peerTable.addPeers(Map(newPeer.id -> newPeer))
      peers <- peerTable.peers
    } yield peers.size shouldBe 1 // not 2
  }

  "removePeers should remove nodes from the table" in {
    for {
      peerTable <- PeerTable[F, Int, PeerNode]
      peer1 = PeerNode(1, Endpoint("localhost"))
      peer2 = PeerNode(1, Endpoint("localhost"))
      _ <- peerTable.addPeers(Map(peer1.id -> peer1))
      _ <- peerTable.addPeers(Map(peer2.id -> peer2))
      _ <- peerTable.removePeers(Set(2))
      peers <- peerTable.peers
    } yield peers shouldBe Map(peer1.id -> peer1)
  }
}
