package slick

import cats.effect.{Async, Resource, Sync}
import org.apache.commons.dbcp2.BasicDataSource
import sdk.syntax.all.*
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect

object PostgresSlickDb {
  def apply[F[_]: Async](dbName: String, dbUser: String, dbPassword: String): Resource[F, SlickDb] =
    Resource
      .make(Sync[F].delay {
        Class.forName("org.postgresql.Driver").void()

        val dataSource = new BasicDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.setUrl(s"jdbc:postgresql://localhost:5432/$dbName")
        dataSource.setUsername(dbUser)
        dataSource.setPassword(dbPassword)
        dataSource.setMaxTotal(20)
        dataSource.setMaxIdle(10)
        dataSource.setInitialSize(10)

        Database.forDataSource(dataSource, None)
      })(db => Sync[F].delay(db.close()))
      .evalMap(SlickDb(_, PostgresProfile, new PostgresDialect))
}
