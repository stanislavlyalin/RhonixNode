package sdk.api.syntax

import cats.effect.kernel.Sync
import cats.syntax.all.*
import fs2.Stream
import sdk.api.FindApi

trait ApiFindSyntax {
  implicit def apiFindSyntax[F[_], Id, T](find: FindApi[F, Id, T]) = new ApiFindOps[F, Id, T](find)
}

final class ApiFindOps[F[_], Id, T](private val find: FindApi[F, Id, T]) extends AnyVal {
  def find(id: Id): F[Option[T]]             = find.find(id, identity)
  def get(id: Id)(implicit F: Sync[F]): F[T] =
    F.flatMap(find.find(id, identity))(_.liftTo[F](new NoSuchElementException(s"Id $id not found")))
  def all: Stream[F, (Id, T)]                = find.findAll((_: Id, _: T) => true)
}
