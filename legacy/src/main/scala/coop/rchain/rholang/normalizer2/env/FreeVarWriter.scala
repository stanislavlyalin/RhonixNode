package coop.rchain.rholang.normalizer2.env

import coop.rchain.rholang.interpreter.compiler.{IdContext, SourcePosition}

trait FreeVarWriter[T] {
  // Free variables operations

  /** Puts free variables to the context */
  def putFreeVar(binding: IdContext[T]): Int

  // Scope operations

  /** Runs functions in an empty free variables scope (preserving history) */
  def withNewFreeVarScope[R](scopeFn: () => R, insideReceive: Boolean = false): R
}

object FreeVarWriter {
  def apply[T](implicit instance: FreeVarWriter[T]): FreeVarWriter[T] = instance
}
