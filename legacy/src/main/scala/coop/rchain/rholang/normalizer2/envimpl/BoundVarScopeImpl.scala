package coop.rchain.rholang.normalizer2.envimpl

import cats.effect.Sync
import coop.rchain.rholang.normalizer2.env.BoundVarScope
import coop.rchain.rholang.syntax.*

final case class BoundVarScopeImpl[F[_]: Sync, T](private val boundMapChain: HistoryChain[VarMap[T]])
    extends BoundVarScope[F] {

  override def withNewBoundVarScope[R](scopeFn: F[R]): F[R] = boundMapChain.runWithNewDataInChain(scopeFn, VarMap.empty)

  override def withCopyBoundVarScope[R](scopeFn: F[R]): F[R] =
    boundMapChain.runWithNewDataInChain(scopeFn, boundMapChain.current())
}
