package slick

import cats.effect.{Async, Resource, Sync}
import org.apache.commons.dbcp2.BasicDataSource
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect

object PostgresSlickDb {
  def apply[F[_]: Async](
    dbUrl: String,
    dbUser: String,
    dbPassword: String,
    initialConnections: Int,
    maxIdleConnections: Int,
    maxTotalConnections: Int,
  ): Resource[F, SlickDb] =
    Resource
      .make(Sync[F].delay {
        val dataSource = new BasicDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.setUrl(dbUrl)
        dataSource.setUsername(dbUser)
        dataSource.setPassword(dbPassword)
        dataSource.setInitialSize(initialConnections)
        dataSource.setMaxIdle(maxIdleConnections)
        dataSource.setMaxTotal(maxTotalConnections)

        Database.forDataSource(dataSource, None)
      })(db => Sync[F].delay(db.close()))
      .evalMap(SlickDb(_, PostgresProfile, new PostgresDialect))
}
