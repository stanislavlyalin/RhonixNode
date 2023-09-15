package db

import cats.effect.{Async, Resource}
import slick.jdbc.JdbcBackend.DatabaseDef

object SlickPgSql {
  def apply[F[_]: Async]: Resource[F, DatabaseDef] = EmbeddedPostgresResource().flatMap { embPostgres =>
    DbDef(embPostgres)
  }
}
