package coop.rchain.rholang.normalizer2.env

import coop.rchain.rholang.interpreter.compiler.IdContext

trait FreeVarWriter[T] {
  // Free variables operations

  /** Puts free variables to the context */
  def putFreeVar(binding: IdContext[T]): Int

  // Scope operations

  /** Runs functions in an empty free variables scope (preserving history)
   * @param insideReceive Flag is necessary for normalizing the connectives.
   * Since we cannot rely on a specific pattern matching order, we cannot use patterns
   * separated by \/ to bind any variables in the top-level receive. */
  def withNewFreeVarScope[R](insideReceive: Boolean)(scopeFn: () => R): R
}

object FreeVarWriter {
  def apply[T](implicit instance: FreeVarWriter[T]): FreeVarWriter[T] = instance
}
