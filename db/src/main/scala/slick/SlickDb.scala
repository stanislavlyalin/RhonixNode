package slick

import cats.data.OptionT
import cats.effect.kernel.Async
import cats.syntax.all.*
import slick.api.SlickApi
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.migration.api.{Dialect, Migration}
import slick.syntax.all.DBIOActionRunSyntax

import scala.util.Try

// Do not expose underlying Database to prevent possibility of calling close() on it
final case class SlickDb private (private val db: Database, profile: JdbcProfile) {
  def run[F[_]: Async, A](a: DBIOAction[A, NoStream, Nothing]): F[A] = Async[F].fromFuture(db.run(a).pure)
}

object SlickDb {
  def apply[F[_]: Async](
    db: Database,
    profile: JdbcProfile,
    dialect: Dialect[?],
  ): F[SlickDb] = {
    def runMigrations(db: Database): F[SlickDb] = {
      implicit val slickDb: SlickDb = new SlickDb(db, profile)
      val key                       = "dbVersion"

      def applyMigration(m: Migration): F[Unit] = Async[F].fromFuture(db.run(m()).pure)

      def applyAllNewerThen(version: Int, api: SlickApi[F]): F[Unit] = migrations
        .all(dialect)
        .collect {
          case (idx, migrations) if idx > version =>
            migrations.migrations.foreach(applyMigration).pure *>
              api.queries.putConfig(key, idx.toString).run
        }
        .toList
        .traverse_(_.void)

      def run: F[Unit] = (for {
        api          <- SlickApi[F](slickDb)
        dbVersionOpt <- api.queries.getConfig(key).run.recover { case _ => "0".some }
        version      <- OptionT
                          .fromOption(Try(dbVersionOpt.getOrElse("0").toInt).toOption)
                          .getOrRaise(new RuntimeException(s"Error reading $key from config table"))
      } yield applyAllNewerThen(version, api)).flatten

      run *> slickDb.pure
    }

    runMigrations(db).as(new SlickDb(db, profile))
  }
}
