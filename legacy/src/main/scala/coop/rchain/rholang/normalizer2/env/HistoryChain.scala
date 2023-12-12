package coop.rchain.rholang.normalizer2.env

final case class HistoryChain[T](chain: collection.mutable.ListBuffer[T]) {

  def current(): T = chain.last

  def depth: Int = chain.length

  def iter: Iterator[T] = chain.iterator

  def push(t: T) = chain.append(t)

  def pushCopy() = this.push(chain.last)

  def pop() = chain.remove(chain.length - 1)
}
