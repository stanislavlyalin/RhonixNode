package node.api

import cats.data.ValidatedNel
import sdk.api.data.{Block, Deploy, Status, TokenTransferRequest}
import sdk.api.{ApiErr, ExternalApi}

class ExternalApiSlickImpl[F[_]] extends ExternalApi[F] {
  override def getBlockByHash(hash: Array[Byte]): F[Option[Block]]                    = ???
  override def getDeployByHash(hash: Array[Byte]): F[Option[Deploy]]                  = ???
  override def getDeploysByHash(hash: Array[Byte]): F[Option[Seq[Array[Byte]]]]       = ???
  override def getBalance(state: Array[Byte], wallet: Array[Byte]): F[Option[Long]]   = ???
  override def getLatestMessages: F[List[Array[Byte]]]                                = ???
  override def status: F[Status]                                                      = ???
  override def transferToken(tx: TokenTransferRequest): F[ValidatedNel[ApiErr, Unit]] = ???
}
