package coop.rchain.rholang.normalizer2.util

import cats.effect.Sync
import cats.implicits.*
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

  override def withNewBoundVarScope[F[_]: Sync, R](scopeFn: () => F[R]): F[R] = for {
    _   <- Sync[F].delay(newScopeLevel += 1)
    res <- scopeFn()
    _   <- Sync[F].delay(newScopeLevel -= 1)
  } yield res

  override def withCopyBoundVarScope[F[_]: Sync, R](scopeFn: () => F[R]): F[R] = for {
    _   <- Sync[F].delay(copyScopeLevel += 1)
    res <- scopeFn()
    _   <- Sync[F].delay(copyScopeLevel -= 1)
  } yield res

  def extractData: Seq[BoundVarWriterData[T]] = buffer.toSeq
  def getNewScopeLevel: Int                   = newScopeLevel
  def getCopyScopeLevel: Int                  = copyScopeLevel

}
