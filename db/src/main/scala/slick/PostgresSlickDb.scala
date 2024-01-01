package slick

import cats.effect.{Async, Resource, Sync}
import sdk.syntax.all.*
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect

object PostgresSlickDb {
  def apply[F[_]: Async](dbName: String, dbUser: String, dbPassword: String): Resource[F, SlickDb] =
    Resource
      .make(Sync[F].delay {
        Class.forName("org.postgresql.Driver").void()

        Database.forURL(
          s"jdbc:postgresql://localhost:5432/$dbName",
          keepAliveConnection = true,
          user = dbUser,
          password = dbPassword,
        )
      })(db => Sync[F].delay(db.close()))
      .evalMap(SlickDb(_, PostgresProfile, new PostgresDialect))
}
