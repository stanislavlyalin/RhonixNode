package node

import sdk.reflect.Description

@Description("node")
final case class Config(
  @Description("Persist on chain state on disk.")
  persistOnChainState: Boolean = false,
  @Description("Limit number of blocks to be processed concurrently")
  processingConcurrency: Int = 4,
  @Description("Enable streaming of metrics to InfluxDb")
  enableInfluxDb: Boolean = false,
  @Description("Enable dev mode. WARNING: This mode is not secure and should not be used in production.")
  devMode: Boolean = false,
  @Description("Database name")
  dbName: String = "gorki_node_db",
  @Description("Database user")
  dbUser: String = "postgres",
  @Description("Database password")
  dbPassword: String = "postgres",
)

object Config {
  val Default: Config = Config()
}
