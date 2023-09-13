package sdk.db

import cats.Applicative
import cats.syntax.all.*

import java.sql.Connection

/// Wrapper for java.sql.Connection to make it injectable as a context bound
trait SqlConn[F[_]] {
  def get: F[java.sql.Connection]
}

object SqlConn {
  def apply[F[_]](implicit ev: SqlConn[F]): SqlConn[F] = ev

  def apply[F[_]: Applicative](sqlConn: Connection): SqlConn[F] = new SqlConn[F] {
    override def get: F[Connection] = sqlConn.pure[F]
  }
}
