package sim.balances

import cats.Parallel
import cats.effect.kernel.{Async, Sync}
import cats.syntax.all.*
import sdk.diag.Metrics
import sdk.diag.Metrics.{Field, Tag}
import sdk.hashing.Blake2b256Hash
import sdk.history.{History, InsertAction}
import sdk.store.KeyValueTypedStore
import sdk.syntax.all.*
import sim.balances.data.BalancesState

/**
 * Builds blockchain state storing balances and provides reading the data.
 * */
trait BalancesStateBuilderWithReader[F[_]] {
  def buildState(
    baseState: Blake2b256Hash,
    toFinalize: BalancesState,
    toMerge: BalancesState,
  ): F[(Blake2b256Hash, Blake2b256Hash)]

  def readBalance(state: Blake2b256Hash, wallet: Wallet): F[Option[Balance]]
}

object BalancesStateBuilderWithReader {

  // Balances state cannot bear store balances
  private def negativeBalanceException(w: Wallet, b: Balance): Exception =
    new Exception(s"Attempt to commit negative balance $b for wallet $w.")

  def apply[F[_]: Async: Parallel: Metrics](
    history: History[F],
    valueStore: KeyValueTypedStore[F, Blake2b256Hash, Balance],
  ): BalancesStateBuilderWithReader[F] = {

    /**
     * Create action for history and persist hash -> value relation.
     *
     * Thought the second is not necessary, value type for RadixHistory is hardcoded with Blake hash,
     * so cannot just place Balance as a value there.
     */
    def createHistoryActionAndStoreData(wallet: Wallet, balance: Balance): F[InsertAction] =
      for {
        _    <- Sync[F].raiseError(negativeBalanceException(wallet, balance)).whenA(balance < 0)
        vHash = balanceToHash(balance)
        _    <- valueStore.put(vHash, balance)
      } yield InsertAction(walletToKeySegment(wallet), vHash)

    def applyActions(
      root: Blake2b256Hash,
      setBalanceActions: List[(Wallet, Balance)],
    ): F[Blake2b256Hash] =
      for {
        h       <- history.reset(root)
        actions <- setBalanceActions.traverse { case (w, b) => createHistoryActionAndStoreData(w, b) }
        root    <- h.process(actions).map(_.root)
      } yield root

    new BalancesStateBuilderWithReader[F] {
      override def buildState(
        baseState: Blake2b256Hash,
        toFinalize: BalancesState,
        toMerge: BalancesState,
      ): F[(Blake2b256Hash, Blake2b256Hash)] = for {
        // merge final state
        finalHash <- applyActions(baseState, toFinalize.diffs.toList).timedM("commit-final-state")
        _         <- Metrics[F].gauge("final-hash", finalHash.bytes.toHex)
        // merge pre state, apply tx on top top get post state
        postState  = toFinalize ++ toMerge
        // merge post state
        postHash  <- applyActions(finalHash, postState.diffs.toList)
      } yield finalHash -> postHash

      override def readBalance(state: Blake2b256Hash, wallet: Wallet): F[Option[Balance]] = for {
        h    <- history.reset(state)
        bOpt <- h.read(walletToKeySegment(wallet))
        r    <- bOpt.flatTraverse(hash => valueStore.get1(hash))
      } yield r
    }
  }
}
