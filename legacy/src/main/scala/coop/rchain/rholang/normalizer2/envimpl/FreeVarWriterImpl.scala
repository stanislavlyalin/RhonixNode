package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.FreeVarWriter
import coop.rchain.rholang.syntax.*
import sdk.syntax.all.*

final case class FreeVarWriterImpl[T](private val freeMapChain: HistoryChain[VarMap[T]]) extends FreeVarWriter[T] {

  override def putFreeVar(binding: IdContext[T]): Int = {
    val (name, typ, sourcePosition) = binding
    // Put the new element into the current map
    freeMapChain.updateCurrent(_.put(name, typ, sourcePosition)).void()
    // Find the index of the recently added element
    // NOTE: get is safe here because we just added the element
    freeMapChain.current().get(name).get.index
  }
}
