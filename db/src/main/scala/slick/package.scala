import slick.lifted.TableQuery
import slick.tables.{TableBonds, TableValidators}

package object slick {

  // all queries
  val bonds      = TableQuery[TableBonds]
  val validators = TableQuery[TableValidators]
}
