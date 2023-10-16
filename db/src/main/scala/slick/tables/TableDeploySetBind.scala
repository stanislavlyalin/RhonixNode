package slick.tables

import slick.jdbc.PostgresProfile.api.*
import slick.lifted.ProvenShape
import slick.tables.TableDeploySetBind.DeploySetBind

class TableDeploySetBind(tag: Tag) extends Table[DeploySetBind](tag, "deploy_set_bind") {
  def deploySetId: Rep[Long] = column[Long]("deploy_set_id")
  def deployId: Rep[Long]    = column[Long]("deploy_id")

  def pk = primaryKey("pk_deploy_set_bind", (deploySetId, deployId))

  def fk1 = foreignKey("fk_deploy_set_bind_deploy_set_id", deploySetId, slick.qDeploySet)(_.id)
  def fk2 = foreignKey("fk_deploy_set_bind_deploy_id", deployId, slick.qDeploy)(_.id)

  def idx = index("idx_deploy_set_bind", deploySetId, unique = false)

  def * : ProvenShape[DeploySetBind] = (deploySetId, deployId).mapTo[DeploySetBind]
}

object TableDeploySetBind {
  final case class DeploySetBind(
    deploySetId: Long, // pointer to deploySet
    deployId: Long,    // pointer to deploy
  )
}
