package sdk

import org.squeryl.dsl.{CanCompare, TByteArray}
import org.squeryl.{KeyedEntity, Schema, Table}
import sdk.db.*

trait DbTable extends KeyedEntity[Long] {
  def name: String = this.getClass.getSimpleName.takeWhile(_ != '$').replace("Table", "")
}

object CustomTypeMode extends org.squeryl.PrimitiveTypeMode {
  // custom types, etc, go here
  implicit val canCompareByteArray: CanCompare[TByteArray, TByteArray] = new CanCompare[TByteArray, TByteArray]
}

import sdk.CustomTypeMode.*

object RhonixNodeDb extends Schema {
  val validatorTable: Table[ValidatorTable]                     = table[ValidatorTable]("Validator")
  val bondTable: Table[BondTable]                               = table[BondTable]("Bond")
  val deployTable: Table[DeployTable]                           = table[DeployTable]("Deploy")
  val blockTable: Table[BlockTable]                             = table[BlockTable]("Block")
  val blockJustificationsTable: Table[BlockJustificationsTable] = table[BlockJustificationsTable]("BlockJustifications")
  val blockDeploysTable: Table[BlockDeploysTable]               = table[BlockDeploysTable]("BlockDeploys")
  val blockBondsTable: Table[BlockBondsTable]                   = table[BlockBondsTable]("BlockBonds")

  autoincrement(validatorTable)
  autoincrement(bondTable)
  autoincrement(deployTable)
  autoincrement(blockTable)

  private def autoincrement[A <: DbTable](table: Table[A]): Unit =
    on(table)(t => declare(t.id is autoIncremented(s"${t.name}_id_seq")))
}

package object db {}
