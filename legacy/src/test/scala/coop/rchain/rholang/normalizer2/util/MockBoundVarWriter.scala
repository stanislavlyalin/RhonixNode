package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.BoundVarWriter
import coop.rchain.rholang.normalizer2.util.Mock.BoundVarWriterData

import scala.collection.mutable.ListBuffer

case class MockBoundVarWriter[T]() extends BoundVarWriter[T] {
  private val buffer: ListBuffer[BoundVarWriterData[T]] = ListBuffer.empty
  private var withNewScopeLevel: Int                    = 0
  private var withCopyScopeLevel: Int                   = 0

  override def putBoundVars(bindings: Seq[IdContext[T]]): Seq[Int] = {
    buffer.appendAll(bindings.map { case (name, varType, _) =>
      BoundVarWriterData(name, varType, withNewScopeLevel, withCopyScopeLevel)
    })
    Seq()
  }

  override def withNewBoundVarScope[R](scopeFn: () => R): R = {
    withNewScopeLevel = withNewScopeLevel + 1
    val res = scopeFn()
    withNewScopeLevel = withNewScopeLevel - 1
    res
  }

  override def withCopyBoundVarScope[R](scopeFn: () => R): R = {
    withCopyScopeLevel = withCopyScopeLevel + 1
    val res = scopeFn()
    withCopyScopeLevel = withCopyScopeLevel - 1
    res
  }

  def extractData: Seq[BoundVarWriterData[T]] = buffer.toSeq
  def newScopeLevel(): Int                    = withNewScopeLevel
  def copyScopeLevel(): Int                   = withCopyScopeLevel

}
