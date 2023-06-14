package sdk

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class DagCausalQueueSpec extends AnyFreeSpec with Matchers {
  // Map of items to dependencies
  val input = Map(
    0 -> Set.empty[Int],
    1 -> Set(0),
    2 -> Set(0),
    3 -> Set(1, 2),
    4 -> Set(1, 2),
  )
  // enqueue all items in arbitrary order
  val q     = input.iterator.foldLeft(DagCausalQueue.default[Int]) { case (acc, (i, d)) => acc.enqueue(i, d) }

  "items with no dependencies should appear in the output" in {
    // take from queue
    val (_, result) = q.dequeue
    // 0 has no dependencies so should be available to dequeue
    result shouldBe Set(0)
  }

  "satisfy should be Noop with false flag when removing an item that is yet to be dequeued" in {
    val (newQ, flag) = q.satisfy(0)
    flag shouldBe false
    newQ shouldBe q
  }

  "satisfy should be Noop with false flag when satisfying item while dependencies are not satisfied yet" in {
    val (newQ, flag) = q.satisfy(1)
    flag shouldBe false
    newQ shouldBe q
  }

  "when nothing to dequeue, empty set should be returned" in {
    q.dequeue._1.dequeue._2 shouldBe Set()
  }

  "dequeue should return set if items with all dependencies satisfied" in {
    // take item with no dependencies (0), otherwise it cannot be satisfied
    val (q1, _)     = q.dequeue
    // remove item 0 - this makes 1 and 2 available for dequeue
    val (q2, flag)  = q1.satisfy(0)
    flag shouldBe true
    // items 1 and 2 should be available for dequeue
    val (_, result) = q2.dequeue
    result shouldBe Set(1, 2)
  }

  "should output valid causal sequence" in {
    q.dequeue._2 shouldBe Set(0)
    q.dequeue._1.satisfy(0)._1.dequeue._2 shouldBe Set(1, 2)
    q.dequeue._1.satisfy(0)._1.dequeue._1.satisfy(1)._1.dequeue._2 shouldBe Set()
    q.dequeue._1.satisfy(0)._1.dequeue._1.satisfy(1)._1.satisfy(2)._1.dequeue._2 shouldBe Set(3, 4)
  }
}
