package coop.rchain.rholang.normalizer2.env

trait NestingInfoWriter[F[_]] {

  /** Run scopeFn with a note that this is a pattern
   * @param inReceive Flag is necessary for normalizing the connectives.
   * Since we cannot rely on a specific pattern matching order, we cannot use patterns
   * separated by \/ to bind any variables in the top-level receive.
   * */
  def markAsPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R]

  /** Run scopeFn with a note that this is a bundle */
  def markAsBundle[R](scopeFn: F[R]): F[R]
}
