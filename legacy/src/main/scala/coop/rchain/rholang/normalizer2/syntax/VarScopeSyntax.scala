package coop.rchain.rholang.normalizer2.syntax

import coop.rchain.rholang.normalizer2.env.{BoundVarScope, FreeVarScope}

trait VarScopeSyntax {
  implicit def varScopeOps[F[_]](scope: BoundVarScope[F]): VarScopeOps[F] = new VarScopeOps[F](scope)
}

final class VarScopeOps[F[_]](val bWScope: BoundVarScope[F]) extends AnyVal {

  /** Creates a new (empty) bound and free variable context/scope.
   *
   * Empty variable context is used to normalize patterns.
   */
  def withNewVarScope[R](insideReceive: Boolean = false)(scopeFn: () => F[R])(implicit fWScope: FreeVarScope[F]): F[R] =
    bWScope.withNewBoundVarScope(() => fWScope.withNewFreeVarScope(insideReceive)(scopeFn))
}
