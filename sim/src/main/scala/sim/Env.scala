package sim

import cats.Applicative
import cats.effect.Ref
import cats.effect.kernel.{Async, Temporal}
import cats.effect.std.Random
import cats.syntax.all.*
import dproc.data.Block
import fs2.{Pipe, Stream}
import io.rhonix.node.Node
import sim.NetworkSim.*
import weaver.data.ConflictResolution

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration.{Duration, DurationInt}

/** Mocks for real world things. */
object Env {

  /** Shared block store across simulation. */
  val blocks: mutable.Map[M, Block.WithId[M, S, T]] = TrieMap.empty[M, Block.WithId[M, S, T]]

  def broadcast[F[_]: Temporal](
    peers: List[(String, Node[F, M, S, T])],
    time: Duration,
  ): Pipe[F, M, Unit] = _.evalMap { x =>
    Temporal[F].sleep(time) >> peers.traverse { case (_, node) =>
      assert(blocks.contains(x), "Save block into Env before broadcasting.")
      node.saveBlock(blocks(x)) *>
        node.dProc.acceptMsg(x)
    }.void
  }

  // ids for messages of particular sender (no equivocation)
  // instead of using cryptographic hashing function
  def dummyIds(sender: S): LazyList[String] =
    LazyList.unfold(Map.empty[S, Int]) { acc =>
      val next   = acc.getOrElse(sender, 0) + 1
      val newAcc = acc + (sender -> next)
      val id     = s"$sender|-$next"
      (id, newAcc).some
    }

  def TPS[F[_]: Async]: Pipe[F, ConflictResolution[T], Int] = { (in: Stream[F, ConflictResolution[T]]) =>
    in.flatMap(x => Stream.emits(x.accepted.toList)).through(measurePerSec)
  }

  def measurePerSec[F[_]: Async, I]: Pipe[F, I, Int] = {
    val acc = Ref.unsafe[F, Int](0)
    val out = Stream.eval(acc.getAndUpdate(_ => 0)).delayBy(1.second).repeat
    (in: Stream[F, I]) => out concurrently in.evalTap(_ => acc.update(_ + 1))
  }
}
