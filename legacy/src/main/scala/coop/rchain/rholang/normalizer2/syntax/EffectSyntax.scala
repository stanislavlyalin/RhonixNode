package coop.rchain.rholang.normalizer2.syntax

import coop.rchain.rholang.normalizer2.env.{BoundVarScope, FreeVarScope}

trait EffectSyntax {
  implicit def normalizerEffectSyntax[F[_], A](f: F[A]): NormalizerEffectOps[F, A] = new NormalizerEffectOps[F, A](f)
}

class NormalizerEffectOps[F[_], A](val f: F[A]) extends AnyVal {

  /**
   * Run effect inside a new (empty) bound and free variable context/scope.
   *
   * Empty variable context is used to normalize patterns.
   */
  def withNewVarScope(
    insideReceive: Boolean = false,
  )(implicit fwScope: FreeVarScope[F], bwScope: BoundVarScope[F]): F[A] =
    bwScope.withNewBoundVarScope(fwScope.withNewFreeVarScope(insideReceive)(f))
}
