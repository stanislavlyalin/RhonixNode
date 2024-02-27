package node

import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.*
import sdk.reflect.{ClassAsTuple, ClassesAsConfig, Description}
import slick.api.SlickApi

import java.nio.file.Path

@Description("node")
final case class Config(
  @Description("Persist on chain state on disk.")
  persistOnChainState: Boolean = false,
  @Description("Path to key value stores folder.")
  kvStoresPath: Path = Path.of("~/.gorki/kv-store"),
  @Description("Limit number of blocks to be processed concurrently")
  processingConcurrency: Int = 4,
  @Description("Enable streaming of metrics to InfluxDb")
  enableInfluxDb: Boolean = false,
  @Description("Enable dev mode. WARNING: This mode is not secure and should not be used in production.")
  devMode: Boolean = false,
  @Description("WebApi configuration")
  webApi: node.api.web.Config = node.api.web.Config("localhost", 8080),
  @Description("Node gRPC port")
  gRpcPort: Int = 5555,
)

object Config {
  val Default: Config = Config()

  def load[F[_]: Async](
    db: SlickApi[F],
  ): Resource[F, (node.Config, diagnostics.metrics.Config, comm.Config)] = Resource.eval(Sync[F].defer {
    val root = "gorki"

    val dbKeyValueMapF = for {
      keys   <- Sync[F].delay(
                  ClassesAsConfig.kvMap(root, node.Config, diagnostics.metrics.Config, comm.Config).keys.toList,
                )
      values <- keys.traverse(db.getConfig)
    } yield keys.zip(values).toMap

    // DB is empty if number of values read from DB is less than total number of fields in Configs
    val dbEmpty: F[Boolean] = for {
      map <- dbKeyValueMapF
    } yield map.collect { case (k, Some(v)) => k -> v }.size < map.size

    val loadFromFileCached         = Sync[F].memoize(loadFromFile)
    val loadFromFileOk: F[Boolean] = loadFromFileCached.flatMap(_.attempt.map(_.isRight))

    val saveToDb: ((node.Config, diagnostics.metrics.Config, comm.Config)) => F[Unit] = {
      case (nodeCfg, diagCfg, commCfg) =>
        ClassesAsConfig
          .kvMap(root, nodeCfg, diagCfg, commCfg)
          .toList
          .traverse { case (k, v) => db.putConfig(k, v) }
          .void
    }

    val loadInitial: F[(node.Config, diagnostics.metrics.Config, comm.Config)] =
      (node.Config.Default, diagnostics.metrics.Config.Default, comm.Config.Default).pure[F]

    val loadFromDB: F[(node.Config, diagnostics.metrics.Config, comm.Config)] = for {
      map  <- dbKeyValueMapF.map(_.collect { case (k, Some(v)) => k -> v })
      nCfg <- ClassAsTuple.fromMap[F, node.Config](root, map)
      dCfg <- ClassAsTuple.fromMap[F, diagnostics.metrics.Config](root, map)
      cCfg <- ClassAsTuple.fromMap[F, comm.Config](root, map)
    } yield (nCfg, dCfg, cCfg)

    dbEmpty.ifM(
      loadFromFileOk.ifM(
        loadFromFileCached.flatMap(_.flatTap(saveToDb)),
        loadInitial.flatTap(saveToDb),
      ),
      loadFromDB,
    )
  })

  private def loadFromFile[F[_]: Sync]: F[(node.Config, diagnostics.metrics.Config, comm.Config)] = {
    import pureconfig.*
    import pureconfig.generic.auto.*

    for {
      configs <- ConfigSource.default
                   .load[(node.Config, diagnostics.metrics.Config, comm.Config)]
                   .leftTraverse[F, (node.Config, diagnostics.metrics.Config, comm.Config)] { err =>
                     new Exception(
                       "Invalid configuration file",
                       new Exception(err.toList.map(_.description).mkString("\n")),
                     )
                       .raiseError[F, (node.Config, diagnostics.metrics.Config, comm.Config)]
                   }
                   .map(_.merge)
    } yield configs
  }
}
