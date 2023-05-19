package weaver.data

/**
  * Finality data.
  * @param fFringe final fringe.
  */
final case class LazoF[M](fFringe: Set[M])
