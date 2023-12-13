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

  def runRepeatedlyOrDefault(default: R, attempts: Int = 100)(implicit F: Async[F], slickDb: SlickDb): F[R] = {
    def repeat[A](action: => F[A], attempts: Int): F[A] = action.handleErrorWith {
      case ex: PSQLException if ex.getSQLState == PSQLState.SERIALIZATION_FAILURE.getState && attempts > 0 =>
        repeat(action, attempts - 1)
      case e                                                                                               => F.raiseError(e)
    }
    repeat(slickDb.run(x), attempts).handleError(_ => default)
  }
}
