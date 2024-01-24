package coop.rchain.rholang.normalizer2.envimpl

import sdk.syntax.all.*

final case class HistoryChain[T](private val chain: collection.mutable.ListBuffer[T]) {

  def current(): T = chain.last

  def depth: Int = chain.length

  def iter: Iterator[T] = chain.iterator

  def push(t: T): Unit = chain.append(t).void()

  def pushCopy(): Unit = this.push(chain.last)

  def pop(): T = chain.remove(chain.length - 1)
}
