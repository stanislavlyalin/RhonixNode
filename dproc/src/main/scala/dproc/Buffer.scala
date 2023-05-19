package dproc

import cats.effect.kernel.{Concurrent, Ref}
import cats.syntax.all._
import dproc.Buffer.ST
import fs2.Stream
import fs2.concurrent.Channel

/**
 * Buffer for messages that cannot be processed yet by [[Processor]] because not all dependencies are processed.
 * @param stRef reference for a buffer state
 * @param accept callback to add message to the buffer: id of a message and ids of all not processed yet
 *               dependencies required
 * @param completed callback to be invoked when [[Processor]] done processing another message
 * @param out stream of messages that have all dependencies processed by [[Processor]], thus ready top be processed.
 */
final case class Buffer[F[_], M](
  stRef: Ref[F, ST[M]],
  accept: (M, Set[M]) => F[Unit],
  completed: M => F[Unit],
  out: Stream[F, M]
)

object Buffer {

  def apply[F[_]: Concurrent, M]: F[Buffer[F, M]] = for {
    stRef <- Ref[F].of(emptyST[M])
    completedCh <- Channel.unbounded[F, M]
    acceptedCh <- Channel.unbounded[F, (M, Set[M])]
  } yield {
    val acceptStream = acceptedCh.stream.evalMap(m => stRef.update(_.add(m._1, m._2)))
    val completeStream = completedCh.stream.evalMap(m => stRef.modify(_.complete(m)))
    val out = completeStream.map(_.toSeq).flatMap(Stream.emits).concurrently(acceptStream)

    new Buffer[F, M](stRef, acceptedCh.send(_, _).void, completedCh.send(_).void, out)
  }

  def emptyST[M]: ST[M] = ST[M](Map(), Map())

  final case class ST[M](childMap: Map[M, Set[M]], missingMap: Map[M, Set[M]]) {

    /** Add message to the buffer. TODO make sure transition from buffer to DAG not influencing this. */
    def add(m: M, missing: Set[M]): ST[M] = {
      val newChildMap = missing.foldLeft(childMap) { case (acc, d) =>
        acc + (d -> acc.get(d).map(_ + m).getOrElse(Set(m)))
      }
      val newMissingMap = if (missing.isEmpty) missingMap else missingMap + (m -> missing)

      ST(newChildMap, newMissingMap)
    }

    /** Completion of message can lead to releasing some message from a buffer. */
    def complete(m: M): (ST[M], Set[M]) = {
      val awaiting = childMap.getOrElse(m, Set())
      val (adjusted, unaltered) = missingMap.view.partition { case (m, _) => awaiting.contains(m) }
      val (done, altered) = adjusted.mapValues(_ - m).partition { case (_, deps) => deps.isEmpty }
      val doneSet = done.keys.toSet

      val newChildMap = childMap - m
      val newMissingMap = (unaltered ++ altered).filterNot { case (k, _) =>
        (doneSet + m).contains(k)
      }.toMap
      ST(newChildMap, newMissingMap) -> doneSet
    }

    def contains(m: M) = childMap.contains(m)
  }
}
