package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.interpreter.compiler.FreeContext
import coop.rchain.rholang.normalizer2.env.{FreeVarReader, VarContext}

final case class FreeVarReaderImpl[T](private val freeMapChain: HistoryChain[VarMap[T]]) extends FreeVarReader[T] {

  override def getFreeVars: Seq[(String, FreeContext[T])] =
    freeMapChain.current().getAll.map { case (name, VarContext(index, typ, sourcePosition)) =>
      (name, FreeContext(index, typ, sourcePosition))
    }

  override def getFreeVar(name: String): Option[FreeContext[T]] = freeMapChain.current().get(name).map {
    case VarContext(index, typ, sourcePosition) => FreeContext(index, typ, sourcePosition)
  }
}
