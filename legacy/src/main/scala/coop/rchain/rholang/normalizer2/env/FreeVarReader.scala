package coop.rchain.rholang.normalizer2.env

import coop.rchain.rholang.interpreter.compiler.FreeContext

trait FreeVarReader[T] {
  // Free variables operations

  /** Gets all free variables */
  def getFreeVars: Seq[FreeContext[T]]

  /** Gets free variable */
  def getFreeVar(name: String): Option[FreeContext[T]]

  // Scope operations

  /** Flag if free variable scope is on top level, meaning not within the pattern */
  def topLevel: Boolean
}

object FreeVarReader {
  def apply[T](implicit instance: FreeVarReader[T]): FreeVarReader[T] = instance
}
