package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TablePeers.Peer

class TablePeers(tag: Tag) extends Table[Peer](tag, "peer") {
  def id: Rep[Long]             = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def url: Rep[String]          = column[String]("url", O.Unique)
  def isSelf: Rep[Boolean]      = column[Boolean]("isSelf")
  def isValidator: Rep[Boolean] = column[Boolean]("isValidator")

  def idx = index("idx_peer_url", url, unique = true)

  override def * : ProvenShape[Peer] = (id, url, isSelf, isValidator).mapTo[Peer]
}

object TablePeers {
  final case class Peer(
    id: Long,
    url: String,
    isSelf: Boolean,
    isValidator: Boolean,
  )
}
