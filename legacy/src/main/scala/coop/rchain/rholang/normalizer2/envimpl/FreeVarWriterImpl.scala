package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.interpreter.compiler.{IdContext, SourcePosition}
import coop.rchain.rholang.normalizer2.env.FreeVarWriter

final case class FreeVarWriterImpl[T](private val putFn: (String, T, SourcePosition) => Int) extends FreeVarWriter[T] {

  override def putFreeVar(binding: IdContext[T]): Int = putFn.tupled(binding)
}
