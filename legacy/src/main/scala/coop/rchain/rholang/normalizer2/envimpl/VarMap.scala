package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.normalizer2.env.VarContext

/** Map that associates variable names with their context, including Bruijn index, type, and source position. */
final class VarMap[T](private val data: Map[String, VarContext[T]], private val nextIndex: Int) {

  /** Retrieve the variable context by its name.
   * @return Some(varContext) if the variable is found, None otherwise. */
  def get(name: String): Option[VarContext[T]] = data.get(name)

  def getAll: Seq[(String, VarContext[T])] = data.toSeq

  def put(name: String, sort: T, sourcePosition: SourcePosition): VarMap[T] = {
    val newData  = data.updated(name, VarContext(nextIndex, sort, sourcePosition))
    val newIndex = nextIndex + 1
    new VarMap(newData, newIndex)
  }
}

object VarMap {
  def empty[T]: VarMap[T] = new VarMap(Map[String, VarContext[T]](), 0)
}
