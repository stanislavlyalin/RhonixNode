package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.BoundVarWriter
import coop.rchain.rholang.normalizer2.util.Mock.BoundVarWriterData

import scala.collection.mutable.ListBuffer

case class MockBoundVarWriter[F[_], T](scope: MockBoundVarScope[F]) extends BoundVarWriter[T] {
  private val buffer: ListBuffer[BoundVarWriterData[T]] = ListBuffer.empty

  override def putBoundVars(bindings: Seq[IdContext[T]]): Seq[Int] = {
    val newScopeLevel  = scope.getNewScopeLevel
    val copyScopeLevel = scope.getCopyScopeLevel
    buffer.appendAll(bindings.map { case (name, varType, _) =>
      BoundVarWriterData(name, varType, newScopeLevel, copyScopeLevel)
    })
    bindings.zipWithIndex.map(_._2)
  }

  def extractData: Seq[BoundVarWriterData[T]] = buffer.toSeq

}
