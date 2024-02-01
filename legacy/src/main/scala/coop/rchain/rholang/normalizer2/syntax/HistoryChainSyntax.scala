package coop.rchain.rholang.normalizer2.syntax

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.normalizer2.envimpl.HistoryChain

trait HistoryChainSyntax {
  implicit def normalizerSyntaxHistoryChain[T](x: HistoryChain[T]): HistoryChainOps[T] =
    HistoryChainOps(x)
}

final case class HistoryChainOps[T](private val x: HistoryChain[T]) extends AnyVal {

  /**
   * Updates the current element in the HistoryChain.
   * @param f a transformation function that takes an element of type `T` and returns a transformed element of the same type
   */
  def updateCurrent(f: T => T): Unit = x.push(f(x.pop()))

  /** Run scopeFn with new data in the HistoryChain. */
  def runWithNewDataInChain[F[_]: Sync, R](scopeFn: F[R], newData: T): F[R] =
    for {
      _   <- Sync[F].delay(x.push(newData))
      res <- scopeFn
      _    = x.pop()
    } yield res
}
