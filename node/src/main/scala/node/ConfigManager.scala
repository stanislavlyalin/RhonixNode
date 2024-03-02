package node

import cats.effect.{Async, Resource, Sync}
import io.circe.parser.parse
import io.circe.{Decoder, Encoder, Json}
import sdk.reflect.ClassesAsConfig
import slick.api.SlickApi
import cats.syntax.all.*
import diagnostics.*

import java.nio.file.Path
import scala.concurrent.duration.Duration

// Logic to store and load config from the database
object ConfigManager {
  val DefaultConfig = (node.Config.Default, metrics.Config.Default, comm.Config.Default)

  // Field to check that database is empty so should be populated with default config
  val EmptyCheckKey = "isEmpty"

  /**
   * Build node configuration.
   *
   * If the database is empty, the default configuration is written to it.
   * If the database is not empty - configuration is loaded from the database.
   * */
  def buildConfig[F[_]: Async](
    db: SlickApi[F],
  ): Resource[F, (node.Config, metrics.Config, comm.Config)] = Resource.eval(Sync[F].defer {
    // Imports containing implicits to derive JSON encoding for config classes
    import io.circe.generic.auto.*
    import io.circe.syntax.*

    // Codecs to represent Path and Duration as strings, Circe does not have this out of the box
    implicit val ePath: Encoder[Path]             = Encoder.encodeString.contramap(_.toAbsolutePath.toString)
    implicit val dPath: io.circe.Decoder[Path]    = io.circe.Decoder.decodeString.map(x => Path.of(x))
    implicit val eDur: Encoder[Duration]          = Encoder.encodeLong.contramap(_.toNanos)
    implicit val dDur: io.circe.Decoder[Duration] = io.circe.Decoder.decodeLong.map(s => Duration.fromNanos(s))

    /**
     * Write config into database
     * @param x instance of a config class. Should be annotated, ptal [[node.Config]]
     * @tparam A type of the config class
     * @return
     */
    def writeConfig[A: Encoder.AsObject](x: A): F[Unit] = {
      val configName = ClassesAsConfig.configName(x)
      x.asJsonObject.toList.traverse_ { case (k, v) => db.putConfig(s"$configName.$k", v.noSpaces) }
    }

    // Update annotated config of type A with records from from database.
    def loadConfig[A: Decoder](x: A): F[A] = {
      def noKeyErr(key: String) = new Exception(s"No config for $key")
      val configName            = ClassesAsConfig.configName(x)
      val keys                  = ClassesAsConfig.fields(x)
      keys
        .traverse { key =>
          val dbKey = s"$configName.$key" // Key in the database is written with config prefix
          for {
            value     <- db.getConfig(dbKey).flatMap(_.liftTo[F](noKeyErr(dbKey)))
            jsonValue <- parse(value).liftTo[F]
          } yield key -> jsonValue
        }
        .flatMap(Json.fromFields(_).as[A].liftTo[F])
    }

    // Write all configs into database and set empty check flag
    def write(nCfg: node.Config, mCfg: metrics.Config, cCfg: comm.Config): F[Unit] =
      writeConfig(nCfg) *> writeConfig(mCfg) *> writeConfig(cCfg) *> db.putConfig(EmptyCheckKey, "")

    // Load all configs from database
    val load: F[(node.Config, metrics.Config, comm.Config)] =
      (loadConfig(node.Config.Default), loadConfig(metrics.Config.Default), loadConfig(comm.Config.Default)).tupled

    for {
      isEmpty <- db.getConfig(EmptyCheckKey).map(_.isEmpty)
      r       <- if (isEmpty) (write _).tupled(DefaultConfig).as(DefaultConfig) else load
    } yield r
  })
}
