package slick

import cats.effect.Resource
import cats.effect.kernel.Async
import cats.syntax.all.*
import org.postgresql.util.PSQLException
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.migration.api.Dialect

// Do not expose underlying Database to prevent possibility of calling close() on it
final case class SlickDb private (private val db: Database, profile: JdbcProfile) {
  def run[F[_]: Async, A](a: DBIOAction[A, NoStream, Nothing]): F[A] = Async[F].fromFuture(db.run(a).pure)
}

object SlickDb {
  def resource[F[_]: Async](
    dbResource: Resource[F, Database],
    profile: JdbcProfile,
    dialect: Dialect[?],
  ): Resource[F, SlickDb] = {
    // Execute migrations each time resource is instantiated
    def runMigration(db: Database): F[Unit] =
      Async[F].fromFuture(db.run(migrations.all(dialect)()).pure).handleErrorWith {
        // Since slick-migration-api has no information about which migrations have been applied,
        // re-applying migrations may throw exception about the existence of an entity in the database
        // SQLState 42P07 means `ERROR: relation "xxx" already exists`
        case e: PSQLException if e.getSQLState == "42P07" => ().pure
        case error                                        => Async[F].raiseError(error)
      }

    dbResource.evalMap(db => runMigration(db).as(new SlickDb(db, profile)))
  }
}
