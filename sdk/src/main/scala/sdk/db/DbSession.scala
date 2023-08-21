package sdk.db

import cats.effect.Sync

trait DbSession[F[_]] {
  def using[A](a: => A): A
}

object DbSession {
  def apply[F[_]](implicit instance: DbSession[F]): instance.type = instance
  def withSessionF[F[_]: Sync: DbSession, A](a: => A): F[A]       = Sync[F].blocking(DbSession[F].using(a))
}
