package sim

import cats.Parallel
import cats.effect.kernel.{Async, Temporal}
import cats.effect.std.Console
import cats.effect.{Ref, Sync}
import cats.syntax.all.*
import dproc.data.Block
import fs2.{Pipe, Stream}
import node.Hashing.*
import node.comm.CommImpl
import node.comm.CommImpl.BlockHash
import node.state.State
import node.{DbApiImpl, Genesis, Setup}
import sdk.data.{BalancesDeploy, BalancesState}
import sdk.diag.Metrics
import sdk.primitive.ByteArray
import sdk.syntax.all.{effectSyntax, fs2StreamSyntax}
import sim.NetworkSnapshot.NodeSnapshot
import slick.syntax.all.slickApiSyntax
import weaver.data.FinalData

import scala.concurrent.duration.{Duration, DurationInt}

object Network {

  def apply[F[_]: Async: Parallel: Console](
    peers: List[Setup[F]],
    genesisPosState: FinalData[ByteArray],
    genesisBalancesState: BalancesState,
    netCfg: sim.Config,
  ): fs2.Stream[F, Unit] = {

    def broadcast(peers: List[Setup[F]], delay: Duration): Pipe[F, ByteArray, Unit] =
      _.evalMap(m => Temporal[F].sleep(delay) *> peers.traverse_(_.ports.sendToInput(BlockHash(m)).void))

    val peersWithIdx = peers.zipWithIndex

    val (streams, diags) = peersWithIdx.map { case setup -> idx =>
      val getSnapshot =
        (
          setup.stateManager.weaverStRef.get,
          setup.stateManager.propStRef.get,
          setup.stateManager.procStRef.get,
          setup.stateManager.bufferStRef.get,
        ).flatMapN { case (w, p, pe, b) =>
          setup.node.lfsHash.map(State(w, p, pe, b, _))
        }

      val tpsRef    = Ref.unsafe[F, Double](0f)
      val tpsUpdate = setup.ports.finStream
        .map(_.accepted.toList)
        .flatMap(Stream.emits(_))
        .throughput(1.second)
        // finality is computed by each sender eventually so / c.size
        .map(_.toDouble / peers.size)
        .evalTap(tpsRef.set)
      val getData   = (getSnapshot, tpsRef.get).mapN { case State(w, p, pe, b, lfsHash) -> tps =>
        NodeSnapshot[ByteArray, ByteArray, BalancesDeploy](
          ByteArray(BigInt(idx).toByteArray),
          tps.toFloat,
          tps.toFloat / netCfg.txPerBlock,
          w,
          p,
          pe,
          b,
          lfsHash,
        )
      }

      val p2pStream = {
        val notSelf = peersWithIdx.filterNot(_._2 == idx).map(_._1)

        // TODO this is a hack to make block appear in peers databases
        def copyBlockToPeers(hash: ByteArray): F[Unit] = for {
          bOpt   <- DbApiImpl(setup.database).readBlock(hash)
          b      <- bOpt.liftTo[F](new Exception(s"Block not found for hash $hash"))
          bWithId = Block.WithId[ByteArray, ByteArray, BalancesDeploy](hash, b)
          _      <- peers.traverse(peerSetup => DbApiImpl(peerSetup.database).saveBlock(bWithId))
        } yield ()

        setup.node.dProc.output
          .evalTap(copyBlockToPeers)
          .through(
            broadcast(notSelf, netCfg.propDelay)
              // add execution delay
              .compose(x => Stream.eval(Async[F].sleep(netCfg.exeDelay).replicateA(netCfg.txPerBlock)) *> x),
          )
      }

      val nodeStream = setup.node.dProc.dProcStream concurrently setup.ports.inHash

      val run = nodeStream concurrently p2pStream concurrently tpsUpdate
      run -> getData
    }.unzip

    val simStream: Stream[F, Unit] = Stream.emits(streams).parJoin(streams.size)

    val logDiag: Stream[F, Unit] = {
      val getNetworkState = diags.sequence
      getNetworkState.showAnimated(samplingTime = 150.milli)
    }

    val mkGenesis: Stream[F, Unit] = {
      val genesisCreator: Setup[F] = peers.head
      implicit val m: Metrics[F]   = Metrics.unit
      Stream.eval(
        Genesis
          .mkGenesisBlock[F](
            genesisPosState.bonds.bonds.head._1,
            genesisPosState,
            genesisBalancesState,
            genesisCreator.balancesShard,
          )
          .flatMap { genesisM =>
            val genesis = genesisM.m.txs.head
            DbApiImpl(genesisCreator.database).saveBlock(genesisM) *>
              DbApiImpl(genesisCreator.database).saveBalancesDeploy(genesis) *>
              genesisCreator.ports.sendToInput(CommImpl.BlockHash(genesisM.id)) *>
              Sync[F].delay(println(s"Genesis block created"))
          },
      )
    }

    simStream concurrently logDiag concurrently mkGenesis
  }
}
