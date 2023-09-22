package io.rhonix.node

import cats.effect.Ref
import cats.effect.kernel.{Async, Sync, Temporal}
import cats.syntax.all.*
import dproc.DProc
import dproc.DProc.ExeEngine
import dproc.data.Block
import io.github.liquibase4s.cats.CatsMigrationHandler.*
import io.github.liquibase4s.{Liquibase, LiquibaseConfig}
import rhonix.execution.OnlyBalancesEngine.DummyExe
import rhonix.execution.{MergePreState, OnlyBalancesEngine}
import sdk.DagCausalQueue
import sdk.api.FindApi
import sdk.node.{Processor, Proposer}
import sdk.syntax.all.*
import weaver.WeaverState
import weaver.data.FinalData

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.Duration

final case class NodeFindApi[F[_], M, S, T](
  // state API
  balances: FindApi[F, S, Long],
  // block API
  blocks: FindApi[F, M, Block[M, S, T]],
  // deploy API
  deploys: FindApi[F, T, OnlyBalancesEngine.Deploy[S]],
)

final case class Node[F[_], M, S, T](
  // state
  weaverStRef: Ref[F, WeaverState[M, S, T]],
  procStRef: Ref[F, Processor.ST[M]],
  propStRef: Ref[F, Proposer.ST],
  bufferStRef: Ref[F, DagCausalQueue[M]],
  // inputs and outputs
  dProc: DProc[F, M, T],
  // query API
  findApi: NodeFindApi[F, M, S, T],
  // blockAPI
  saveBlock: Block.WithId[M, S, T] => F[Unit],
)

object Node {

  /** Make instance of a process - peer or the network.
   * Init with last finalized state (lfs as the simplest). */
  def apply[F[_]: Async, M, S, T: Ordering](
    id: S,
    lfs: WeaverState[M, S, T],
    lfsExe: FinalData[S],
    hash: Block[M, S, T] => F[M],
    exeDelay: Duration,
    stateReadTime: Duration,
    txMap: T => OnlyBalancesEngine.Deploy[S],
    txPool: Ref[F, List[T]],
  ): F[Node[F, M, S, T]] = {
    // TODO blocks and deploys DB
    val blockStore = TrieMap.empty[M, Block[M, S, T]]

    val blockApi = new FindApi[F, M, Block[M, S, T]] {
      override def find[R](id: M, proj: Block[M, S, T] => R): F[Option[R]] =
        Sync[F].delay(blockStore.get(id).map(proj))

      override def findAll(proj: (M, Block[M, S, T]) => Boolean): fs2.Stream[F, (M, Block[M, S, T])] =
        fs2.Stream.fromIterator(blockStore.iterator.filter(proj.tupled), 1)
    }

    val saveBlock = (b: Block.WithId[M, S, T]) => blockStore.update(b.id, b.m).pure[F]

    val deployPool = TrieMap.empty[T, OnlyBalancesEngine.Deploy[S]]

    val deployPoolApi = new FindApi[F, T, OnlyBalancesEngine.Deploy[S]] {
      override def find[R](id: T, proj: OnlyBalancesEngine.Deploy[S] => R): F[Option[R]] =
        deployPool.get(id).map(proj).pure[F]

      override def findAll(
        proj: (T, OnlyBalancesEngine.Deploy[S]) => Boolean,
      ): fs2.Stream[F, (T, OnlyBalancesEngine.Deploy[S])] =
        fs2.Stream.fromIterator(deployPool.iterator.filter(proj.tupled), 1)
    }

    def loadTx: F[Set[T]] = txPool.modify(l => (l.tail, Set(l.head)))

    for {
      weaverStRef    <- Ref.of(lfs)                       // weaver
      proposerStRef  <- Ref.of(Proposer.default)          // proposer
      processorStRef <- Ref.of(Processor.default[M]())    // processor
      bufferStRef    <- Ref.of(DagCausalQueue.default[M]) // buffer

      exeEngine = new ExeEngine[F, M, S, T] {
                    def execute(
                      base: Set[M],
                      fringe: Set[M],
                      toFinalize: Set[T],
                      toMerge: Set[T],
                      txs: Set[T],
                    ): F[((Blake2b256Hash, Seq[T]), (Blake2b256Hash, Seq[T]))] =
                      computePreStateWithEffects(base, fringe, toFinalize, toMerge, txs)

                    // data read from the final state associated with the final fringe
                    def consensusData(fringe: Set[M]): F[FinalData[S]] = lfs.lazo.trustAssumption.pure[F] // TODO
                  }

      dproc <- DProc.apply[F, M, S, T](
                 weaverStRef,
                 proposerStRef,
                 processorStRef,
                 bufferStRef,
                 loadTx,
                 id.some,
                 exeEngine,
                 Relation.notRelated[F, T],
                 hash,
                 saveBlock,
                 blockApi.get,
               )

    } yield {
      val api = NodeFindApi[F, M, S, T](
        balances = balanceAPI,
        blocks = blockApi,
        deploys = deployPoolApi,
      )
      new Node(
        weaverStRef,
        processorStRef,
        proposerStRef,
        bufferStRef,
        dproc,
        api,
        saveBlock,
      )
    }
  }

  /** Example of programmatically applying liquibase migrations */
  private def applyDBMigrations[F[_]: Sync](user: String, password: String): F[Unit] = {
    val config: LiquibaseConfig = LiquibaseConfig(
      url = "jdbc:postgresql://localhost:5432/rhonixnode",
      user = user,
      password = password,
      driver = "org.postgresql.Driver",
      changelog = "db/changelog.yaml",
    )
    Liquibase[F](config).migrate()
  }
}
