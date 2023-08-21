package sdk.api

import fs2.Stream

/** Find some piece of data with projection. */
trait FindApi[F[_], Id, T] {
  def find[R](id: Id, proj: T => R): F[Option[R]]
  def findAll(proj: (Id, T) => Boolean): Stream[F, (Id, T)]
}
