package slick

import cats.effect.{Async, Resource, Sync}
import db.Config
import org.apache.commons.dbcp2.BasicDataSource
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile
import slick.migration.api.PostgresDialect

object PostgresSlickDb {
  def apply[F[_]: Async](config: Config): Resource[F, SlickDb] =
    Resource
      .make(Sync[F].delay {
        val dataSource = new BasicDataSource()
        dataSource.setDriverClassName("org.postgresql.Driver")
        dataSource.setUrl(config.dbUrl)
        dataSource.setUsername(config.dbUser)
        dataSource.setPassword(config.dbPassword)
        dataSource.setInitialSize(config.initialConnections)
        dataSource.setMaxIdle(config.maxIdleConnections)
        dataSource.setMaxTotal(config.maxTotalConnections)

        Database.forDataSource(dataSource, None)
      })(db => Sync[F].delay(db.close()))
      .evalMap(SlickDb(_, PostgresProfile, new PostgresDialect))
}
