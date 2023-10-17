package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableShards.Shard

class TableShards(tag: Tag) extends Table[Shard](tag, "shard") {
  def id: Rep[Long]     = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name: Rep[String] = column[String]("name")

  def idx = index("idx_shard", name, unique = true)

  def * : ProvenShape[Shard] = (id, name).mapTo[Shard]
}

object TableShards {
  final case class Shard(
    id: Long,    // primary key
    name: String,// shard name
  )
}
