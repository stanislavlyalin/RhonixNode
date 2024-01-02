package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.BoundVarWriter
import coop.rchain.rholang.normalizer2.util.Mock.BoundVarWriterData

import scala.collection.mutable.ListBuffer

case class MockBoundVarWriter[T]() extends BoundVarWriter[T] {
  private val buffer: ListBuffer[BoundVarWriterData[T]] = ListBuffer.empty
  private var newScopeLevel: Int                        = 0
  private var copyScopeLevel: Int                       = 0

  override def putBoundVars(bindings: Seq[IdContext[T]]): Seq[Int] = {
    buffer.appendAll(bindings.map { case (name, varType, _) =>
      BoundVarWriterData(name, varType, newScopeLevel, copyScopeLevel)
    })
    Seq()
  }

  override def withNewBoundVarScope[R](scopeFn: () => R): R = {
    newScopeLevel = newScopeLevel + 1
    val res = scopeFn()
    newScopeLevel = newScopeLevel - 1
    res
  }

  override def withCopyBoundVarScope[R](scopeFn: () => R): R = {
    copyScopeLevel = copyScopeLevel + 1
    val res = scopeFn()
    copyScopeLevel = copyScopeLevel - 1
    res
  }

  def extractData: Seq[BoundVarWriterData[T]] = buffer.toSeq
  def getNewScopeLevel: Int                   = newScopeLevel
  def getCopyScopeLevel: Int                  = copyScopeLevel

}
