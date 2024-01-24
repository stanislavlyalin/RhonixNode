package coop.rchain.rholang.normalizer2.envimpl

import cats.effect.Sync
import coop.rchain.rholang.normalizer2.env.FreeVarScope
import coop.rchain.rholang.syntax.*

final case class FreeVarScopeImpl[F[_]: Sync, T](private val freeMapChain: HistoryChain[VarMap[T]])
    extends FreeVarScope[F] {

  override def withNewFreeVarScope[R](scopeFn: F[R]): F[R] = freeMapChain.runWithNewDataInChain(scopeFn, VarMap.empty)
}
