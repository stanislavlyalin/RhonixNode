package sdk.api

import cats.data.ValidatedNel
import sdk.api.data.*

trait ExternalApi[F[_]] {
  // block given its hash
  def getBlockByHash(hash: Array[Byte]): F[Option[Block]]
  // deploy given its hash
  def getDeployByHash(hash: Array[Byte]): F[Option[Deploy]]
  // deploy signatures given hash of their signatures
  def getDeploysByHash(hash: Array[Byte]): F[Option[Seq[Array[Byte]]]]
  // balance of a vault at particular state
  def getBalance(state: Array[Byte], wallet: Array[Byte]): F[Option[Long]]
  // latest messages in the database
  def getLatestMessages: F[List[Array[Byte]]]
  // status of the node
  def status: F[Status]
  // submit transaction
  def transferToken(tx: TokenTransferRequest): F[ValidatedNel[ApiErr, Unit]]
}
