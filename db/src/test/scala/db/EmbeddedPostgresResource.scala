package db

import cats.effect.{Resource, Sync}
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

object EmbeddedPostgresResource {
  def apply[F[_]: Sync](): Resource[F, EmbeddedPostgres] = Resource.make {
    Sync[F].delay(EmbeddedPostgres.builder().start())
  }(x => Sync[F].delay(x.close()))
}
