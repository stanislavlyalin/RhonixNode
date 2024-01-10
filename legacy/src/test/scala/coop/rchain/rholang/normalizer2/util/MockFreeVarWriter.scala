package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.FreeVarWriter
import coop.rchain.rholang.normalizer2.util.Mock.{DefFreeVarIndex, FreeVarWriterData}

import scala.collection.mutable.ListBuffer

case class MockFreeVarWriter[F[_], T](scope: MockFreeVarScope[F]) extends FreeVarWriter[T] {
  private val buffer: ListBuffer[FreeVarWriterData[T]] = ListBuffer.empty

  override def putFreeVar(binding: IdContext[T]): Int = {
    val scopeLevel = scope.getScopeLevel
    buffer.append(binding match { case (name, varType, _) => FreeVarWriterData(name, varType, scopeLevel) })
    DefFreeVarIndex
  }

  def extractData: Seq[FreeVarWriterData[T]] = buffer.toSeq
}
