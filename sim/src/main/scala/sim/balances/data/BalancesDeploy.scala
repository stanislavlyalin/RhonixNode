package sim.balances.data

import sdk.codecs.Digest
import sdk.primitive.ByteArray

/**
 * Deploy is a state diff with id.
 * */
final case class BalancesDeploy(id: ByteArray, state: BalancesState) {
  override def equals(obj: Any): Boolean = obj match {
    case BalancesDeploy(id, _) => id == this.id
    case _                     => false
  }

  override def hashCode(): Int = id.hashCode()
}

object BalancesDeploy {
  implicit val ordDeploy: Ordering[BalancesDeploy] = Ordering.by(_.id)

  def apply(state: BalancesState, nonce: Long)(implicit d: Digest[(BalancesState, Long)]): BalancesDeploy = {
    val id = d.digest((state, nonce))
    new BalancesDeploy(id, state)
  }
}
