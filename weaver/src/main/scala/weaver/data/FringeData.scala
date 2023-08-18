package weaver.data

/**
 * Finality data.
 * @param fFringe final fringe.
 */
final case class FringeData[M](fFringe: Set[M])

object FringeData { def empty[M]: FringeData[M] = FringeData(Set.empty[M]) }
