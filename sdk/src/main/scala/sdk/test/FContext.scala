package sdk.test

import cats.effect.IO
import org.scalatest.Assertion

import scala.concurrent.Future

trait FContext {
  type F[A] = IO[A]
  import cats.effect.unsafe.implicits.global
  implicit def toFuture(assertion: IO[Assertion]): Future[Assertion] = assertion.unsafeToFuture()
}
