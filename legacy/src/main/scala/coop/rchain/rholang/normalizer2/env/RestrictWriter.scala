package coop.rchain.rholang.normalizer2.env

trait RestrictWriter[F[_]] {

  /** Run scopeFn with restricted conditions for the pattern
   * @param inReceive Flag is necessary for normalizing the connectives.
   * Since we cannot rely on a specific pattern matching order, we cannot use patterns
   * separated by \/ to bind any variables in the top-level receive.
   * */
  def restrictAsPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R]

  /** Run scopeFn with restricted conditions with restrictions as for the bundle */
  def restrictAsBundle[R](scopeFn: F[R]): F[R]
}
