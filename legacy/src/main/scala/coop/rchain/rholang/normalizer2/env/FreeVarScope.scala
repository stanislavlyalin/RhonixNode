package coop.rchain.rholang.normalizer2.env

trait FreeVarScope[F[_]] {

  /** Runs functions in an empty free variables scope (preserving history)
   * @param insideReceive Flag is necessary for normalizing the connectives.
   * Since we cannot rely on a specific pattern matching order, we cannot use patterns
   * separated by \/ to bind any variables in the top-level receive. */
  def withNewFreeVarScope[R](insideReceive: Boolean)(scopeFn: F[R]): F[R]
}

object FreeVarScope {
  def apply[F[_]](implicit instance: FreeVarScope[F]): FreeVarScope[F] = instance
}
