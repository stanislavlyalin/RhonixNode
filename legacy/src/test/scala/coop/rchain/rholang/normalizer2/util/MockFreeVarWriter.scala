package coop.rchain.rholang.normalizer2.util

import cats.effect.Sync
import cats.implicits.*
import coop.rchain.rholang.interpreter.compiler.IdContext
import coop.rchain.rholang.normalizer2.env.FreeVarWriter
import coop.rchain.rholang.normalizer2.util.Mock.{DefFreeVarIndex, FreeVarWriterData}

import scala.collection.mutable.ListBuffer

case class MockFreeVarWriter[T]() extends FreeVarWriter[T] {
  private val buffer: ListBuffer[FreeVarWriterData[T]] = ListBuffer.empty
  private var scopeLevel: Int                          = 0

  override def putFreeVar(binding: IdContext[T]): Int = {
    buffer.append(binding match { case (name, varType, _) => FreeVarWriterData(name, varType, scopeLevel) })
    DefFreeVarIndex
  }

  override def withNewFreeVarScope[F[_]: Sync, R](insideReceive: Boolean = false)(scopeFn: () => F[R]): F[R] = for {
    _   <- Sync[F].delay(scopeLevel += 1)
    res <- scopeFn()
    _   <- Sync[F].delay(scopeLevel -= 1)
  } yield res

  def extractData: Seq[FreeVarWriterData[T]] = buffer.toSeq
  def getScopeLevel: Int                     = scopeLevel
}
