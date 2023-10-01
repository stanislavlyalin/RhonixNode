package sdk.primitive

import cats.effect.kernel.Async
import cats.syntax.all.*

import scala.concurrent.Future

trait FutureSyntax {
  implicit def futureSyntax[A](x: Future[A]): FutureOps[A] = new FutureOps[A](x)
}

final class FutureOps[A](private val x: Future[A]) extends AnyVal {
  def toEffect[F[_]: Async]: F[A] = Async[F].fromFuture(x.pure)
}
