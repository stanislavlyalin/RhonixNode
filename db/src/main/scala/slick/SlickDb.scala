package slick

import cats.effect.{Resource, Sync}
import cats.effect.kernel.Async
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.migration.api.Dialect
import cats.syntax.all.*
import sdk.syntax.all.*

final case class SlickDb private (db: Database, profile: JdbcProfile, dialect: Dialect[?])

object SlickDb {
  def apply[F[_]: Async](
    dbResource: Resource[F, Database],
    profile: JdbcProfile,
    dialect: Dialect[?],
  ): Resource[F, SlickDb] = {
    // Execute migrations each time resource is instantiated
    def runMigration(db: Database) = db.run(migrations.all(dialect)()).toEffect

    dbResource.flatTap(db => Resource.eval(runMigration(db))).map(new SlickDb(_, profile, dialect))
  }
}
