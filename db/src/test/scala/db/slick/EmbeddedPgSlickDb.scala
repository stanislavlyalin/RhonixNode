package db.slick

import cats.effect.Resource
import cats.effect.kernel.Async
import db.EmbeddedPostgresResource
import slick.SlickDb
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect

// Slick interface for embedded Postgres. Only for testing.
object EmbeddedPgSlickDb {
  def apply[F[_]: Async]: Resource[F, SlickDb] = {
    val dbResource = EmbeddedPostgresResource().map { epg =>
      Database.forURL(
        url = epg.getJdbcUrl("postgres", "postgres"),
        driver = "org.postgresql.Driver",
      )
    }

    SlickDb(dbResource, PostgresProfile, new PostgresDialect)
  }
}
