import slick.lifted.TableQuery
import slick.tables.*

package object slick {

  // all queries
  val qBlock         = TableQuery[TableBlock]
  val qBlockSet      = TableQuery[TableBlockSet]
  val qBlockSetBind  = TableQuery[TableBlockSetBind]
  val qBond          = TableQuery[TableBond]
  val qBondSet       = TableQuery[TableBondSet]
  val qBondSetBind   = TableQuery[TableBondSetBind]
  val qDeploy        = TableQuery[TableDeploy]
  val qDeployer      = TableQuery[TableDeployer]
  val qDeploySet     = TableQuery[TableDeploySet]
  val qDeploySetBind = TableQuery[TableDeploySetBind]
  val qShard         = TableQuery[TableShard]
  val qValidator     = TableQuery[TableValidator]
  val config         = TableQuery[TableConfig]
}
