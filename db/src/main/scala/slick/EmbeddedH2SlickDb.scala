package slick

import cats.effect.kernel.Resource
import cats.effect.{Async, Sync}
import slick.jdbc.H2Profile
import slick.jdbc.JdbcBackend.Database
import slick.migration.api.H2Dialect
import cats.implicits.catsSyntaxApply

object EmbeddedH2SlickDb {
  def apply[F[_]: Async]: Resource[F, SlickDb] =
    Resource.liftK(Sync[F].delay(Class.forName("org.h2.Driver"))) *> {
      val open       = Sync[F].delay(Database.forURL("jdbc:h2:mem:test", keepAliveConnection = true))
      val dbResource = Resource.make(open)(db => Sync[F].delay(db.close()))
      SlickDb.resource(dbResource, H2Profile, new H2Dialect)
    }
}
