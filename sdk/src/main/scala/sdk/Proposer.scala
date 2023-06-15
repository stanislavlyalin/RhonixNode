package sdk

import sdk.Proposer.*

final case class Proposer(status: ProposerStatus) {
  def start: (Proposer, Boolean) = status match {
    // proposal should be possible only if the previous one is done
    case Idle     => new Proposer(Creating) -> true
    case Creating => this                   -> false
    case Adding   => this                   -> false
  }

  def created: Proposer = status match {
    case Creating => new Proposer(Adding)
    // should not happen so this is Noop
    case Idle     => this
    case Adding   => this
  }

  def done: Proposer = status match {
    case Adding   => new Proposer(Idle)
    // should not happen so this is Noop
    case Idle     => this
    case Creating => this
  }
}

object Proposer {
  def default: Proposer = new Proposer(Idle)

  sealed trait ProposerStatus

  // ready to create a block
  case object Idle     extends ProposerStatus
  // proposal is being created
  case object Creating extends ProposerStatus
  // proposal is created, awaiting validation and adding to local block dag state
  case object Adding   extends ProposerStatus
}
