package slick.syntax

import cats.effect.kernel.Async
import slick.dbio.{DBIOAction, NoStream}
import sdk.syntax.all.*
import slick.jdbc.JdbcBackend.Database

trait DatabaseSyntax {
  implicit def slickDatabaseSyntax(x: Database): DatabaseOps = new DatabaseOps(x)
}

final class DatabaseOps(private val x: Database) extends AnyVal {
  def runF[F[_]: Async, A](a: DBIOAction[A, NoStream, Nothing]): F[A] = x.run(a).toEffect
}
