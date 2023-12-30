package slick

import cats.effect.kernel.Resource
import cats.effect.{Async, Sync}
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect

object EmbeddedPgSqlSlickDb {
  def apply[F[_]: Async]: Resource[F, SlickDb] = {
    val open       = Sync[F].delay(
      EmbeddedPostgres
        .builder()
        .setOutputRedirector(ProcessBuilder.Redirect.to(new java.io.File("/tmp/embedPgSql.log")))
        .start(),
    )
    val dbResource =
      Resource.make(open)(db => Sync[F].delay(db.close())).map(x => Database.forDataSource(x.getPostgresDatabase, None))
    SlickDb.resource(dbResource, PostgresProfile, new PostgresDialect)
  }
}
