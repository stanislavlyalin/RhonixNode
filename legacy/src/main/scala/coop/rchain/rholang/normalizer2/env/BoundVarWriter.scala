package coop.rchain.rholang.normalizer2.env

import coop.rchain.rholang.interpreter.compiler.IdContext

trait BoundVarWriter[T] {

  /** Inserts new variables in bound map.
   *  @return the number of inserted non-duplicate variables
   * */
  def putBoundVars(bindings: Seq[IdContext[T]]): Int
}

object BoundVarWriter {
  def apply[T](implicit instance: BoundVarWriter[T]): BoundVarWriter[T] = instance
}
