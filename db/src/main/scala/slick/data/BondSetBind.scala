package slick.data

/** Table for linking bondSets and bonds.
 * Pairs (bondSetId, bondId) should be unique.
 */
final case class BondSetBind(
  bondSetId: Long, // pointer to bondSet
  bondId: Long,    // pointer to bond
)
