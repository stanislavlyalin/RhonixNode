package coop.rchain.rholang.normalizer2.envimpl

import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.BoundContext
import coop.rchain.rholang.normalizer2.env.*

final case class BoundVarReaderImpl[T](private val boundMapChain: HistoryChain[VarMap[T]]) extends BoundVarReader[T] {

  override def getBoundVar(name: String): Option[BoundContext[T]] = boundMapChain.current().get(name).map {
    case VarContext(index, typ, sourcePosition) => BoundContext(index, typ, sourcePosition)
  }

  override def findBoundVar(name: String): Option[(BoundContext[T], Int)] =
    // For each `boundMap` in the `boundMapChain`, along with its depth (index)
    boundMapChain.iter.zipWithIndex.toSeq.collectFirstSome { case (boundMap, depth) =>
      // Try to get the variable with the given name from the `boundMap`
      boundMap.get(name).map { case VarContext(index, typ, sourcePosition) =>
        // return first match, along with the depth (index)
        (BoundContext(index, typ, sourcePosition), depth)
      }
    }
}
