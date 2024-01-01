package slick

import cats.effect.kernel.Resource
import cats.effect.{Async, Sync}
import sdk.syntax.all.*
import slick.jdbc.H2Profile
import slick.jdbc.JdbcBackend.Database
import slick.migration.api.H2Dialect

object EmbeddedH2SlickDb {
  def apply[F[_]: Async]: Resource[F, SlickDb] =
    Resource
      .make(Sync[F].delay {
        Class.forName("org.h2.Driver").void()

        Database.forURL("jdbc:h2:mem:test", keepAliveConnection = true)
      })(db => Sync[F].delay(db.close()))
      .evalMap(SlickDb(_, H2Profile, new H2Dialect))
}
