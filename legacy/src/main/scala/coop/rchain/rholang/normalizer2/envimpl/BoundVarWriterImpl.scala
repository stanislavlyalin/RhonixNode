package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.BoundVarWriter
import coop.rchain.rholang.syntax.*
import sdk.syntax.all.*

final case class BoundVarWriterImpl[T](private val boundMapChain: HistoryChain[VarMap[T]]) extends BoundVarWriter[T] {
  override def putBoundVars(bindings: Seq[IdContext[T]]): Seq[Int] = {
    bindings.foreach { case (name, typ, sourcePosition) =>
      boundMapChain.updateCurrent(_.put(name, typ, sourcePosition)).void()
    }
    val nonDuplicatedNames = bindings.map(_._1).distinct
    // Find the indexes of the recently added elements
    // NOTE: Using `get` is safe here because we just added these elements.
    nonDuplicatedNames.map(name => boundMapChain.current().get(name).get.index)
  }
}
