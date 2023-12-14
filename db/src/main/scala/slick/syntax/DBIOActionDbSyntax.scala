package slick.syntax

import cats.effect.kernel.Async
import cats.syntax.all.*
import org.postgresql.util.{PSQLException, PSQLState}
import slick.SlickDb
import slick.dbio.{DBIOAction, Effect, NoStream}

trait DBIOActionRunSyntax {
  implicit def DBIOActionRunSyntax[F[_], R, S <: NoStream, E <: Effect](
    x: DBIOAction[R, S, E],
  ): DBIOSyntaxOps[F, R, S, E] = new DBIOSyntaxOps[F, R, S, E](x)
}

final class DBIOSyntaxOps[F[_], R, +S <: NoStream, -E <: Effect](val x: DBIOAction[R, S, E]) extends AnyVal {
  def run(implicit F: Async[F], slickDb: SlickDb): F[R] = slickDb.run(x)

  def runOrDefault(default: R)(implicit F: Async[F], slickDb: SlickDb): F[R] =
    slickDb
      .run(x)
      .handleErrorWith {
        case ex: PSQLException if ex.getSQLState == PSQLState.UNIQUE_VIOLATION.getState => slickDb.run(x)
        case error                                                                      => F.raiseError(error)
      }
      .recover(_ => default)
}
