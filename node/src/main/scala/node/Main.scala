package node

import cats.Monad
import cats.effect.std.Env
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import sdk.codecs.Base16
import sdk.hashing.Blake2b
import sdk.primitive.ByteArray
import sdk.reflect.ClassesAsConfig
import sdk.syntax.all.*
import slick.SlickPgDatabase
import weaver.data.{Bonds, FinalData}

object Main extends IOApp {
  private val DbUrlKey = "gorki.db.url"
  private val DbUser   = "gorki.db.user"
  private val DbPasswd = "gorki.db.passwd"
  private val NodeId   = "gorki.node.id"

  private val DefaultNodeId = "defaultNodeId"

  private def configWithEnv[F[_]: Env: Monad]: F[(db.Config, String)] = for {
    dbUrlOpt      <- Env[F].get(DbUrlKey)
    dbUserOpt     <- Env[F].get(DbUser)
    dbPasswordOpt <- Env[F].get(DbPasswd)
    nodeIdOpt     <- Env[F].get(NodeId)
  } yield {
    val newCfg = for {
      dbUrl      <- dbUrlOpt
      dbUser     <- dbUserOpt
      dbPassword <- dbPasswordOpt
      nodeId     <- nodeIdOpt
    } yield db.Config.Default.copy(dbUrl = dbUrl, dbUser = dbUser, dbPassword = dbPassword) -> nodeId

    newCfg.getOrElse(db.Config.Default -> DefaultNodeId)
  }

  override def run(args: List[String]): IO[ExitCode] =
    args match {
      case List("--print-default-config") =>
        val referenceConf = ClassesAsConfig(
          "gorki",
          db.Config.Default,
          node.Config.Default,
        )
        IO.println(referenceConf).as(ExitCode.Success)
      case _                              =>
        configWithEnv[IO].flatMap { case (dbCfg, nodeId) =>
          // Todo read bonds file
          val genesisPoS = {
            val rnd               = new scala.util.Random()
            /// Genesis data
            val lazinessTolerance = 1 // c.lazinessTolerance
            val senders           =
              Iterator
                .range(0, 100)
                .map(_ => Array(rnd.nextInt().toByte))
                .map(Blake2b.hash256)
                .map(ByteArray(_))
                .toSet
            // Create lfs message, it has no parents, sees no offences and final fringe is empty set
            val genesisBonds      = Bonds(senders.map(_ -> 100L).toMap)
            FinalData(genesisBonds, lazinessTolerance, 10000)
          }

          val idBa = Base16.decode(nodeId).map(ByteArray(_)).getUnsafe
          val db   = SlickPgDatabase[IO](dbCfg)

          fs2.Stream
            .resource(Setup.all[IO](db, idBa, genesisPoS).map(Node.stream))
            .flatten
            .compile
            .drain
            .map(_ => ExitCode.Success)
            .handleError(_ => ExitCode.Error)
        }
    }
}
