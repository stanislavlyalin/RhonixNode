package db.slick

import cats.effect.kernel.Async
import cats.effect.{Resource, Sync}
import slick.SlickDb
import slick.jdbc.{H2Profile, JdbcBackend}
import slick.jdbc.JdbcBackend.Database
import slick.migration.api.H2Dialect

// Slick interface for embedded Postgres. Only for testing.
object EmbeddedH2SlickDb {
  def apply[F[_]: Async]: Resource[F, SlickDb] = {
    val dbResource = Resource.make(Sync[F].delay(Database.forURL("jdbc:h2:mem:")))(db => Sync[F].delay(db.close()))

    SlickDb(dbResource, H2Profile, new H2Dialect)
  }
}
