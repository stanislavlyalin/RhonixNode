package node

import cats.Monad
import cats.effect.kernel.Ref.Make
import cats.effect.std.{Env, Random}
import cats.effect.{ExitCode, IO, IOApp, Resource, Sync}
import cats.syntax.all.*
import node.Hashing.*
import sdk.codecs.Base16
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.hashing.Blake2b
import sdk.primitive.ByteArray
import sdk.reflect.ClassesAsConfig
import sdk.syntax.all.*
import slick.SlickPgDatabase
import weaver.data.{Bonds, FinalData}
import node.BuildInfo

object Main extends IOApp {
  private val DbUrlKey     = "gorki_db_url"
  private val DbUser       = "gorki_db_user"
  private val DbPasswd     = "gorki_db_passwd"
  private val ValidatorKey = "gorki_validator_sKey"

  private val DefaultNodeId = "e211d1b7e8018b567af18dd25377cb4eb0cf2688eb49aba505682b1ddb647a4e"

  def randomDeploys[F[_]: Make: Sync: Random](
    users: Set[ByteArray],
    n: Int,
  ): F[Set[BalancesDeploy]] = {
    val mkDeploy = for {
      txVal <- Random[F].nextLongBounded(100)
      from  <- Random[F].elementOf(users)
      to    <- Random[F].elementOf(users - from)
    } yield {
      val st = new BalancesState(Map(from -> -txVal, to -> txVal))
      val bd = BalancesDeployBody(st, 0)
      val id = bd.digest
      BalancesDeploy(id, bd)
    }
    mkDeploy.replicateA(n).map(_.toSet)
  }

  private def configWithEnv[F[_]: Env: Monad]: F[(db.Config, String)] = for {
    dbUrlOpt      <- Env[F].get(DbUrlKey)
    dbUserOpt     <- Env[F].get(DbUser)
    dbPasswordOpt <- Env[F].get(DbPasswd)
    nodeIdOpt     <- Env[F].get(ValidatorKey)
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
          val version = s"Node ${BuildInfo.version} (${BuildInfo.gitHeadCommit.getOrElse("commit # unknown")})"
          println(version)

          /// Genesis data
          val genesisPoS = {
            val lazinessTolerance = 1
            // Todo load bonds file
            val rnd               = new scala.util.Random()
            val senders           =
              Iterator
                .range(0, 100)
                .map(_ => Array(rnd.nextInt().toByte))
                .map(Blake2b.hash256)
                .map(ByteArray(_))
                .toSet
            val genesisBonds      = Bonds(senders.map(_ -> 100L).toMap)
            FinalData(genesisBonds, lazinessTolerance, 10000)
          }

          // TODO load wallets file
          val users           = (1 to 100).map(x => ByteArray(x.toByte)).toSet
          val genesisBalances =
            new BalancesState(users.map(_ -> Long.MaxValue / 2).toMap)

          // TODO load PK/SK
          val nodePK = Base16.decode(nodeId).map(ByteArray(_)).getUnsafe

          fs2.Stream
            .resource(Resource.eval(Random.scalaUtilRandom[IO]).flatMap { implicit rnd =>
              Setup.all[IO](SlickPgDatabase[IO](dbCfg), nodePK, genesisPoS, randomDeploys[IO](users, 1))
            })
            .flatMap(Node.apply(nodePK, true, _, genesisPoS, genesisBalances))
            .compile
            .drain
            .map(_ => ExitCode.Success)
        }
    }
}
