package db

import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import slick.jdbc.JdbcBackend.{Database, DatabaseDef}
import slick.migration.api.*
import slick.tables.{BondTableSlick, ValidatorTableSlick}
import slick.tx

object EmbeddedPostgresResource {
  def apply[F[_]: Sync](): Resource[F, EmbeddedPostgres] = Resource.make {
    Sync[F].delay(EmbeddedPostgres.builder().start())
  }(embPostgres => Sync[F].delay(embPostgres.close()))
}

object DbDef {
  def apply[F[_]: Async](embPostgres: EmbeddedPostgres): Resource[F, DatabaseDef] = Resource.make {
    for {
      dbDef <- Sync[F]
                 .delay(
                   Database.forURL(
                     url = embPostgres.getJdbcUrl("postgres", "postgres"),
                     driver = "org.postgresql.Driver",
                   ),
                 )
      _     <- applySlickMigrations(dbDef)
    } yield dbDef
  }(dbDef => Sync[F].delay(dbDef.close()))

  private def applySlickMigrations[F[_]: Async](db: DatabaseDef): F[Unit] = {
    implicit val dialect: PostgresDialect = new PostgresDialect

    val validatorTable = TableMigration(ValidatorTableSlick.validatorTableSlick).create
      .addColumns(_.id, _.publicKey, _.http)
      .addIndexes(_.idx)

    val bondTable = TableMigration(BondTableSlick.bondTableSlick).create
      .addColumns(_.id, _.validatorId, _.stake)

    val migration = validatorTable & bondTable

    tx(db.run(migration()))
  }
}
