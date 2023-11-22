package coop.rchain.rholang.normalizer2.env

import coop.rchain.rholang.interpreter.compiler.{FreeContext, IdContext}

trait BoundVarWriter[T] {
  // Bound variables operations

  /** Inserts new variables in bound map  */
  def putBoundVar(bindings: Seq[IdContext[T]]): Seq[Int]

  def absorbFree(binders: Seq[FreeContext[T]]): Unit

  // Scope operations

  /** Runs functions in an empty bound variables scope (preserving history) */
  def withNewBoundVarScope[R](scopeFn: () => R): R

  /** Runs functions in an copy of this bound variables scope (preserving history) */
  def withCopyBoundVarScope[R](scopeFn: () => R): R
}

object BoundVarWriter {
  def apply[T](implicit instance: BoundVarWriter[T]): BoundVarWriter[T] = instance
}
