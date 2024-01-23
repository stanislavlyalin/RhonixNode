package db

import sdk.reflect.Description

@Description("dbCfg")
final case class Config(
  @Description("Database URL")
  dbUrl: String = "jdbc:postgresql://localhost:5432/gorki_node_db",
  @Description("Database user")
  dbUser: String = "postgres",
  @Description("Database password")
  dbPassword: String = "postgres",
  @Description("Initial size of the DB connection pool")
  initialConnections: Int = 10,
  @Description("Maximum number of DB connections that can remain idle in the pool")
  maxIdleConnections: Int = 10,
  @Description("Maximum total number of idle and borrows DB connections that can be active at the same time")
  maxTotalConnections: Int = 20,
)

object Config {
  val Default: Config = Config()
}
