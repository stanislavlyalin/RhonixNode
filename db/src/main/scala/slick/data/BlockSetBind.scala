package slick.data

/** Table for linking blockSets and blocks.
 * Pairs (blockSetId, blockId) should be unique.
 */
final case class BlockSetBind(
  blockSetId: Long, // pointer to blockSet
  blockId: Long,    // pointer to block
)
