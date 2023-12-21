package slick

import cats.effect.{Async, Resource, Sync}
import cats.implicits.catsSyntaxApply
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect

object PostgresSlickDb {
  def apply[F[_]: Async](dbName: String, dbUser: String, dbPassword: String): Resource[F, SlickDb] =
    Resource.liftK(Sync[F].delay(Class.forName("org.postgresql.Driver"))) *> {
      val open       = Sync[F].delay(
        Database.forURL(
          s"jdbc:postgresql://localhost:5432/$dbName",
          keepAliveConnection = true,
          user = dbUser,
          password = dbPassword,
        ),
      )
      val dbResource = Resource.make(open)(db => Sync[F].delay(db.close()))
      SlickDb.resource(dbResource, PostgresProfile, new PostgresDialect)
    }
}
