package slick

import cats.effect.kernel.Resource
import cats.effect.{Async, Sync}
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect
import slick.util.AsyncExecutor

object EmbeddedPgSqlSlickDb {
  def apply[F[_]: Async]: Resource[F, SlickDb] = {
    val open           = Sync[F].delay(
      EmbeddedPostgres
        .builder()
        .setOutputRedirector(ProcessBuilder.Redirect.to(new java.io.File("/tmp/embedPgSql.log")))
        .start(),
    )
    val maxConnections = 100
    val minThreads     = 100
    val maxThreads     = 100
    val queueSize      = 1000
    val dbResource     =
      Resource
        .make(open)(db => Sync[F].delay(db.close()))
        .map(x =>
          Database.forDataSource(
            x.getPostgresDatabase,
            maxConnections = Some(maxConnections),
            executor = AsyncExecutor("EmbeddedPgExecutor", minThreads, maxThreads, queueSize, maxConnections),
          ),
        )

    dbResource.evalMap(SlickDb(_, PostgresProfile, new PostgresDialect))
  }
}
