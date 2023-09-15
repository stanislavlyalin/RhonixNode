import cats.effect.{Async, Sync}

import scala.concurrent.Future

package object slick {
  def tx[F[_]: Async, R](code: => Future[R]): F[R] = Async[F].fromFuture(Sync[F].blocking(code))
}
