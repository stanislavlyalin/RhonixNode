package rhonix.execution

import cats.effect.kernel.{Async, Sync}
import cats.syntax.all.*
import cats.{Applicative, Monoid}
import fs2.Stream
import sdk.api.FindApi
import sdk.merging.Relation

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{Duration, FiniteDuration}

object OnlyBalancesEngine {

  // For this dummy version signatures are not required - just balances.
  final case class Deploy[S](diffs: Map[S, Long]) extends AnyVal

  // For case of just moving tokens processed deploy and submitted tx are the same - just changes for balances.
  private type ProcessedDeploy[S] = Deploy[S]

  // Monoid implementation for block merge.
  implicit def monoidForOnlyBalancesTupleSpaceProcessedDeploy[S]: Monoid[ProcessedDeploy[S]] =
    new Monoid[ProcessedDeploy[S]] {
      override def empty: ProcessedDeploy[S] = new ProcessedDeploy(Map.empty[S, Long])

      override def combine(x: ProcessedDeploy[S], y: ProcessedDeploy[S]): ProcessedDeploy[S] =
        new ProcessedDeploy(x.diffs |+| y.diffs)
    }

  // All transactions are mergeable and independent
  def relationEverythingIndependent[F[_]: Applicative, T]: Relation[F, T] =
    new Relation[F, T] {
      override def conflicts(l: T, r: T): F[Boolean] = false.pure[F]

      override def depends(x: T, on: T): F[Boolean] = false.pure[F]
    }

  object DummyExe {
    def apply[F[_]: Async, T, S](exeDelay: Duration, fullTX: T => ProcessedDeploy[S]): (
      MergePreState[F, T],
      FindApi[F, S, Long],
    ) = {
      // Deploys indices created after execution.
      val txIndices = TrieMap[T, ProcessedDeploy[S]]()

      val LFS = TrieMap.empty[S, Long]

      def executeDummy(
        mergeFinalSet: Set[T],
        mergeConflictSet: Set[T],
        execute: Set[T],
      ) = Sync[F].defer {
        val newFinal         = mergeFinalSet.toList.map(txIndices).combineAll    // merge final state
        // TODO proper update
        val _                = newFinal.diffs.foreachEntry { case (k, diff) =>
          LFS.update(k, LFS.getOrElse(k, 0L) + diff)
        }
        val _                = mergeConflictSet.map(txIndices).toList.combineAll // merge pre state
        val burnSingleThread = Stream.repeatEval(Sync[F].delay((1 to 1000).toList.combineAll))
        val _                = burnSingleThread.interruptAfter(FiniteDuration(exeDelay._1, exeDelay._2)).compile.last.as(true)
        execute.foreach(x => txIndices.update(x, fullTX(x)))
        true.pure[F]
      }

      val api = new FindApi[F, S, Long] {
        override def find[R](id: S, proj: Long => R): F[Option[R]] = LFS.get(id).map(proj).pure[F]

        override def findAll(proj: (S, Long) => Boolean): Stream[F, (S, Long)] =
          fs2.Stream.fromIterator(LFS.iterator, 1)
      }

      (executeDummy, api)
    }
  }
}
