package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.FreeVarWriter
import coop.rchain.rholang.normalizer2.util.Mock.{DefFreeVarIndex, FreeVarWriterData}

import scala.collection.mutable.ListBuffer

case class MockFreeVarWriter[T]() extends FreeVarWriter[T] {
  private val buffer: ListBuffer[FreeVarWriterData[T]] = ListBuffer.empty
  private var withNewScopeLevel: Int                   = 0

  override def putFreeVar(binding: IdContext[T]): Int = {
    buffer.append(binding match { case (name, varType, _) => FreeVarWriterData(name, varType, withNewScopeLevel) })
    DefFreeVarIndex
  }

  override def withNewFreeVarScope[R](scopeFn: () => R, insideReceive: Boolean = false): R = {
    withNewScopeLevel = withNewScopeLevel + 1
    val res = scopeFn()
    withNewScopeLevel = withNewScopeLevel - 1
    res
  }

  def extractData: Seq[FreeVarWriterData[T]] = buffer.toSeq
  def newScopeLevel(): Int                   = withNewScopeLevel
}
