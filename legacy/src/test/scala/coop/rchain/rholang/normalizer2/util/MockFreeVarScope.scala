package coop.rchain.rholang.normalizer2.util

import cats.effect.Sync
import cats.implicits.*
import coop.rchain.rholang.normalizer2.env.FreeVarScope

case class MockFreeVarScope[F[_]: Sync]() extends FreeVarScope[F] {
  private var scopeLevel: Int = 0

  override def withNewFreeVarScope[R](scopeFn: F[R]): F[R] = for {
    _   <- Sync[F].delay(scopeLevel += 1)
    res <- scopeFn
    _    = scopeLevel -= 1
  } yield res

  def getScopeLevel: Int = scopeLevel
}
