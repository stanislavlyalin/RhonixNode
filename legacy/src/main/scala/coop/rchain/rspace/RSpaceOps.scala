package coop.rchain.rspace

import cats.Applicative
import cats.effect.{Async, Ref, Sync}
import cats.syntax.all.*
import com.typesafe.scalalogging.Logger
import coop.rchain.metrics.implicits.*
import coop.rchain.metrics.{Metrics, Span}
import coop.rchain.rspace.concurrent.ConcurrentTwoStepLockF
import coop.rchain.rspace.hashing.{Blake2b256Hash, StableHashProvider}
import coop.rchain.rspace.history.*
import coop.rchain.rspace.internal.*
import coop.rchain.rspace.trace.{COMM, Consume, Log as EventLog, Produce}
import coop.rchain.shared.{Log, Serialize}
import sdk.log.LogSourceMacroInstance.logSource

import scala.collection.SortedSet
import scala.util.Random

abstract class RSpaceOps[F[_]: Async: Log: Metrics: Span, C, P, A, K](
  historyRepository: HistoryRepository[F, C, P, A, K],
  val storeRef: Ref[F, HotStore[F, C, P, A, K]],
)(implicit
  serializeC: Serialize[C],
  serializeP: Serialize[P],
  serializeA: Serialize[A],
  serializeK: Serialize[K],
) extends SpaceMatcher[F, C, P, A, K] {

  override def syncF: Sync[F] = Sync[F]

  override def spanF: Span[F] = Span[F]

  type MaybeProduceCandidate = Option[ProduceCandidate[C, P, A, K]]
  type MaybeActionResult     = Option[(ContResult[C, P, K], Seq[Result[C, A]])]
  type CandidateChannels     = Seq[C]

  implicit class MapOps(underlying: Map[Produce, Int]) {
    def putAndIncrementCounter(elem: Produce): Map[Produce, Int] =
      underlying
        .get(elem)
        .fold(underlying.+((elem, 1)))(currentCount => underlying.+((elem, currentCount + 1)))
  }

  implicit class RichFSeq[M[_]: Applicative, D](t: M[Seq[D]]) {
    def shuffleWithIndex: M[Seq[(D, Int)]] =
      t.map(d => Random.shuffle(d.zipWithIndex))
  }

  def assertF(predicate: Boolean, errorMsg: => String): F[Unit] =
    Sync[F].raiseError(new IllegalStateException(errorMsg)).unlessA(predicate)

  protected[this] val eventLog: Ref[F, EventLog]                = Ref.unsafe(Seq.empty)
  protected[this] val produceCounter: Ref[F, Map[Produce, Int]] =
    Ref.unsafe(Map.empty[Produce, Int].withDefaultValue(0))

  protected[this] def produceCounters(produceRefs: Seq[Produce]): F[Map[Produce, Int]] =
    for {
      counter <- produceCounter.get
    } yield produceRefs.map(p => p -> counter(p)).toMap

  private val lockF = new ConcurrentTwoStepLockF[F, Blake2b256Hash](MetricsSource)

  // private[this] val installSpanLabel         = Metrics.Source(MetricsSource, "install")
  // private[this] val restoreInstallsSpanLabel = Metrics.Source(MetricsSource, "restore-installs")
  // private[this] val createSoftCheckpointSpanLabel =
  //  Metrics.Source(MetricsSource, "create-soft-checkpoint")
  private[this] val revertSoftCheckpointSpanLabel =
    Metrics.Source(MetricsSource, "revert-soft-checkpoint")
  private[this] val resetSpanLabel                = Metrics.Source(MetricsSource, "reset")

  protected[this] val consumeCommLabel     = "comm.consume"
  protected[this] val consumeTimeCommLabel = "comm.consume-time"
  protected[this] val produceCommLabel     = "comm.produce"
  protected[this] val produceTimeCommLabel = "comm.produce-time"

  protected val historyRepositoryRef: Ref[F, HistoryRepository[F, C, P, A, K]] =
    Ref.unsafe(historyRepository)

  protected[this] val logger: Logger

  // TODO: provide ref instance in the constructor
  private[this] val installs: Ref[F, Installs[F, C, P, A, K]] =
    Ref.unsafe(Map.empty[Seq[C], Install[F, P, A, K]])

  def historyRepo: F[HistoryRepository[F, C, P, A, K]] = historyRepositoryRef.get

  def store: F[HotStore[F, C, P, A, K]] = storeRef.get

  def getData(channel: C): F[Seq[Datum[A]]] =
    store.flatMap(_.getData(channel))

  def getWaitingContinuations(channels: Seq[C]): F[Seq[WaitingContinuation[P, K]]] =
    store.flatMap(_.getContinuations(channels))

  def getJoins(channel: C): F[Seq[Seq[C]]] =
    store.flatMap(_.getJoins(channel))

  protected[this] def consumeLockF(
    channels: Seq[C],
  )(
    thunk: => F[MaybeActionResult],
  ): F[MaybeActionResult] = {
    val hashes = channels.map(ch => StableHashProvider.hash(ch))
    lockF.acquire(hashes)(() => hashes.pure[F])(thunk)
  }

  protected[this] def produceLockF(
    channel: C,
  )(
    thunk: => F[MaybeActionResult],
  ): F[MaybeActionResult] =
    lockF.acquire(Seq(StableHashProvider.hash(channel)))(() =>
      store.flatMap(_.getJoins(channel)).map(_.flatten.map(StableHashProvider.hash(_))),
    )(thunk)

  protected[this] def installLockF(
    channels: Seq[C],
  )(
    thunk: => F[Option[(K, Seq[A])]],
  ): F[Option[(K, Seq[A])]] = {
    val hashes = channels.map(ch => StableHashProvider.hash(ch))
    lockF.acquire(hashes)(() => hashes.pure[F])(thunk)
  }

  protected[this] def storeWaitingContinuation(
    channels: Seq[C],
    wc: WaitingContinuation[P, K],
  ): F[MaybeActionResult] =
    for {
      hotStore <- store
      _        <- hotStore.putContinuation(channels, wc)
      _        <- channels.traverse(channel => hotStore.putJoin(channel, channels))
      _        <- Log[F].debug(s"""|consume: no data found,
                            |storing <(patterns, continuation): (${wc.patterns}, ${wc.continuation})>
                            |at <channels: ${channels}>""".stripMargin.replace('\n', ' '))
    } yield None

  protected[this] def storeData(
    channel: C,
    data: A,
    persist: Boolean,
    produceRef: Produce,
  ): F[MaybeActionResult] =
    for {
      _        <- Log[F].debug(s"produce: no matching continuation found")
      hotStore <- store
      _        <- hotStore.putDatum(channel, Datum(data, persist, produceRef))
      _        <- Log[F].debug(s"produce: persisted <data: $data> at <channel: $channel>")
    } yield None

  protected[this] def storePersistentData(
    dataCandidates: Seq[ConsumeCandidate[C, A]],
    peeks: SortedSet[Int],
  ): F[List[Unit]] =
    dataCandidates.toList
      .sortBy(_.datumIndex)(Ordering[Int].reverse)
      .traverse {
        case ConsumeCandidate(
              candidateChannel,
              Datum(_, persistData, _),
              _,
              dataIndex,
            ) =>
          store.flatMap(_.removeDatum(candidateChannel, dataIndex)).unlessA(persistData)
      }

  def restoreInstalls(): F[Unit] =
    /*spanF.trace(restoreInstallsSpanLabel)*/
    installs.get.flatMap(_.toList.traverse_ { case (channels, Install(patterns, continuation)) =>
      install(channels, patterns, continuation)
    })

  override def consume(
    channels: Seq[C],
    patterns: Seq[P],
    continuation: K,
    persist: Boolean,
    peeks: SortedSet[Int] = SortedSet.empty,
  ): F[MaybeActionResult] =
    if (channels.isEmpty) {
      val msg = "channels can't be empty"
      Log[F].error(msg) >> Sync[F]
        .raiseError[MaybeActionResult](new IllegalArgumentException(msg))
    } else if (channels.length =!= patterns.length) {
      val msg = "channels.length must equal patterns.length"
      Log[F].error(msg) >> Sync[F]
        .raiseError[MaybeActionResult](new IllegalArgumentException(msg))
    } else
      (for {
        consumeRef <- Sync[F].delay(Consume(channels, patterns, continuation, persist))
        result     <- consumeLockF(channels) {
                        lockedConsume(
                          channels,
                          patterns,
                          continuation,
                          persist,
                          peeks,
                          consumeRef,
                        )
                      }
      } yield result).timer(consumeTimeCommLabel)(Metrics[F], MetricsSource)

  protected[this] def lockedConsume(
    channels: Seq[C],
    patterns: Seq[P],
    continuation: K,
    persist: Boolean,
    peeks: SortedSet[Int],
    consumeRef: Consume,
  ): F[MaybeActionResult]

  override def produce(
    channel: C,
    data: A,
    persist: Boolean,
  ): F[MaybeActionResult] =
    (for {
      produceRef <- Sync[F].delay(Produce(channel, data, persist))
      result     <- produceLockF(channel)(
                      lockedProduce(channel, data, persist, produceRef),
                    )
    } yield result).timer(produceTimeCommLabel)(Metrics[F], MetricsSource)

  protected[this] def lockedProduce(
    channel: C,
    data: A,
    persist: Boolean,
    produceRef: Produce,
  ): F[MaybeActionResult]

  override def install(
    channels: Seq[C],
    patterns: Seq[P],
    continuation: K,
  ): F[Option[(K, Seq[A])]] =
    /* spanF.trace(installSpanLabel) */
    installLockF(channels) {
      implicit val ms = MetricsSource
      lockedInstall(channels, patterns, continuation).timer("install-time")
    }

  private[this] def lockedInstall(
    channels: Seq[C],
    patterns: Seq[P],
    continuation: K,
  ): F[Option[(K, Seq[A])]] =
    if (channels.length =!= patterns.length) {
      val msg = "channels.length must equal patterns.length"
      Log[F].error(msg) *> Sync[F].raiseError(new IllegalArgumentException(msg))
    } else {
      /*
       * Here, we create a cache of the data at each channel as `channelToIndexedData`
       * which is used for finding matches.  When a speculative match is found, we can
       * remove the matching datum from the remaining data candidates in the cache.
       *
       * Put another way, this allows us to speculatively remove matching data without
       * affecting the actual store contents.
       */

      for {
        _                    <- Log[F].debug(
                                  s"install: searching for data matching <patterns: $patterns> at <channels: $channels>",
                                )
        consumeRef            = Consume(channels, patterns, continuation, true)
        channelToIndexedData <- channels
                                  .traverse { c =>
                                    store.flatMap(_.getData(c).shuffleWithIndex.map(c -> _))
                                  }
        options              <- extractDataCandidates(channels.zip(patterns), channelToIndexedData.toMap, Nil)
                                  .map(_.sequence)
        result               <- options match {
                                  case None    =>
                                    for {
                                      _        <- installs.update(_.updated(channels, Install(patterns, continuation)))
                                      hotStore <- store
                                      _        <- hotStore.installContinuation(
                                                    channels,
                                                    WaitingContinuation(
                                                      patterns,
                                                      continuation,
                                                      persist = true,
                                                      SortedSet.empty,
                                                      consumeRef,
                                                    ),
                                                  )
                                      _        <- channels.traverse { channel =>
                                                    hotStore.installJoin(channel, channels)
                                                  }
                                      _        <-
                                        Log[F].debug(
                                          s"storing <(patterns, continuation): ($patterns, $continuation)> at <channels: $channels>",
                                        )
                                    } yield None
                                  case Some(_) =>
                                    Sync[F].raiseError(
                                      new RuntimeException("Installing can be done only on startup"),
                                    )
                                }
      } yield result
    }

  def toMap: F[Map[Seq[C], Row[P, A, K]]] = storeRef.get.flatMap(_.toMap)

  override def reset(root: Blake2b256Hash): F[Unit] = spanF.trace(resetSpanLabel) {
    for {
      history       <- historyRepositoryRef.get
      nextHistory   <- history.reset(root)
      _             <- historyRepositoryRef.set(nextHistory)
      _             <- eventLog.set(Seq.empty)
      _             <- produceCounter.set(Map.empty.withDefaultValue(0))
      historyReader <- nextHistory.getHistoryReader(root)
      _             <- createNewHotStore(historyReader)
      _             <- restoreInstalls()

      // Clean channel locks
      _ <- lockF.cleanUp
    } yield ()
  }

  override def clear(): F[Unit] = reset(History.emptyRootHash)

  protected def createNewHotStore(
    historyReader: HistoryReader[F, Blake2b256Hash, C, P, A, K],
  ): F[Unit] =
    for {
      nextHotStore <- HotStore(historyReader.base)
      _            <- storeRef.set(nextHotStore)
    } yield ()

  override def createSoftCheckpoint(): F[SoftCheckpoint[C, P, A, K]] =
    /*spanF.trace(createSoftCheckpointSpanLabel) */
    for {
      store    <- storeRef.get
      cache    <- store.snapshot
      log      <- eventLog.getAndSet(Seq.empty)
      pCounter <- produceCounter.getAndSet(Map.empty.withDefaultValue(0))
    } yield SoftCheckpoint[C, P, A, K](cache, log, pCounter)

  override def revertToSoftCheckpoint(checkpoint: SoftCheckpoint[C, P, A, K]): F[Unit] =
    spanF.trace(revertSoftCheckpointSpanLabel) {
      for {
        history       <- historyRepositoryRef.get
        historyReader <- history.getHistoryReader(history.root)
        hotStore      <- HotStore(checkpoint.cacheSnapshot, historyReader.base)
        _             <- storeRef.set(hotStore)
        _             <- eventLog.set(checkpoint.log)
        _             <- produceCounter.set(checkpoint.produceCounter)
      } yield ()
    }

  def wrapResult(
    channels: Seq[C],
    wk: WaitingContinuation[P, K],
    consumeRef: Consume,
    dataCandidates: Seq[ConsumeCandidate[C, A]],
  ): MaybeActionResult =
    Some(
      (
        ContResult(
          wk.continuation,
          wk.persist,
          channels,
          wk.patterns,
          wk.peeks.nonEmpty,
        ),
        dataCandidates
          .map(dc => Result(dc.channel, dc.datum.a, dc.removedDatum, dc.datum.persist)),
      ),
    )

  def removeMatchedDatumAndJoin(
    channels: Seq[C],
    dataCandidates: Seq[ConsumeCandidate[C, A]],
  ): F[Seq[Unit]] =
    dataCandidates
      .sortBy(_.datumIndex)(Ordering[Int].reverse)
      .traverse { case ConsumeCandidate(candidateChannel, Datum(_, persistData, _), _, dataIndex) =>
        store
          .flatMap(_.removeDatum(candidateChannel, dataIndex))
          .whenA(dataIndex >= 0 && !persistData) >>
          store.flatMap(_.removeJoin(candidateChannel, channels))
      }

  protected[this] def runMatcherForChannels(
    groupedChannels: Seq[CandidateChannels],
    fetchMatchingContinuations: (CandidateChannels) => F[Seq[(WaitingContinuation[P, K], Int)]],
    fetchMatchingData: C => F[(C, Seq[(Datum[A], Int)])],
  ): F[MaybeProduceCandidate] = {
    def go(
      acc: Seq[CandidateChannels],
    ): F[Either[Seq[CandidateChannels], MaybeProduceCandidate]] =
      acc match {
        case channels :: remaining =>
          for {
            matchCandidates          <- fetchMatchingContinuations(channels)
            channelToIndexedDataList <- channels.traverse {
                                          c: C =>
                                            fetchMatchingData(c)
                                        }
            firstMatch               <- extractFirstMatch(
                                          channels,
                                          matchCandidates,
                                          channelToIndexedDataList.toMap,
                                        )
          } yield firstMatch match {
            case None             => remaining.asLeft[MaybeProduceCandidate]
            case produceCandidate => produceCandidate.asRight[Seq[CandidateChannels]]
          }
        case _                     =>
          none[ProduceCandidate[C, P, A, K]].asRight[Seq[CandidateChannels]].pure[F]
      }

    groupedChannels.tailRecM(go)
  }

  protected def logComm(
    dataCandidates: Seq[ConsumeCandidate[C, A]],
    channels: Seq[C],
    wk: WaitingContinuation[P, K],
    comm: COMM,
    label: String,
  ): F[COMM]

  protected def logConsume(
    consumeRef: Consume,
    channels: Seq[C],
    patterns: Seq[P],
    continuation: K,
    persist: Boolean,
    peeks: SortedSet[Int],
  ): F[Consume]

  protected def logProduce(
    produceRef: Produce,
    channel: C,
    data: A,
    persist: Boolean,
  ): F[Produce]
}