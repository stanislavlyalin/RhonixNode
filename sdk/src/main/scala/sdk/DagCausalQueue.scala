package sdk

/**
 * Queue aligning incoming items against causal order.
 *
 * - Each item enqueued has to have zero or more dependencies.
 * - Item can be dequeued only when it has no dependencies or for all dependencies `satisfy` is called.
 * - Item can be dequeued only after all dependencies are dequeued.
 */
final class DagCausalQueue[T](childrenMap: Map[T, Set[T]], parentsMap: Map[T, Set[T]], out: Set[T]) {

  /**
   * Enqueue an item and record its dependencies.
   *
   * @return updated DagCausalQueue.
   */
  def enqueue(x: T, dependencies: Set[T]): DagCausalQueue[T] = {
    val newChildrenMap          = dependencies.foldLeft(childrenMap) { case (acc, d) =>
      acc + (d -> acc.get(d).map(_ + x).getOrElse(Set(x)))
    }
    val (newMissingMap, newOut) =
      if (dependencies.isEmpty) parentsMap -> (out + x) else (parentsMap + (x -> dependencies)) -> out
    new DagCausalQueue(newChildrenMap, newMissingMap, newOut)
  }

  /**
   * Satisfy a dependency.
   *
   * If the argument is awaiting dequeue or some dependency of the argument is not satisfied yet -
   * this call is Noop and false is returned along with state unchanged.
   *
   * @return updated DagCausalQueue and success flag.
   */
  def satisfy(x: T): (DagCausalQueue[T], Boolean) =
    if (parentsMap.get(x).exists(_.nonEmpty) || out.contains(x)) this -> false
    else {
      val awaiting              = childrenMap.getOrElse(x, Set())
      val (adjusted, unaltered) = parentsMap.view.partition { case (m, _) => awaiting.contains(m) }
      val (done, altered)       = adjusted.mapValues(_ - x).partition { case (_, d) => d.isEmpty }
      val doneSet               = done.keys.toSet

      val newChildMap   = childrenMap - x
      val newMissingMap = (unaltered ++ altered).filterNot { case (k, _) => (doneSet + x).contains(k) }.toMap

      val newNext = if (doneSet.nonEmpty) out ++ doneSet else out
      new DagCausalQueue(newChildMap, newMissingMap, newNext) -> true
    }

  /**
   * Dequeue items with all dependencies satisfied.
   *
   * @return updated DagCausalQueue and Set of items with all dependencies satisfied (possibly empty).
   */
  def dequeue: (DagCausalQueue[T], Set[T]) = new DagCausalQueue(this.childrenMap, this.parentsMap, Set()) -> out
}

object DagCausalQueue {
  def default[T]: DagCausalQueue[T] = new DagCausalQueue[T](Map(), Map(), Set())
}
