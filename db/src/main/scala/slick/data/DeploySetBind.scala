package slick.data

/** Table for linking deploySets and deploys.
 * Pairs (deploySetId, deployId) should be unique.
 */
final case class DeploySetBind(
  deploySetId: Long, // pointer to deploySet
  deployId: Long,    // pointer to deploy
)
