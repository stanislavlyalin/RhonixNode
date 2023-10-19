package node.api

import sdk.api.ExternalApi
import sdk.api.data.{Block, Deploy, Status}

class ExternalApiImpl[F[_]] extends ExternalApi[F] {
  override def getBlockByHash(hash: Array[Byte]): F[Option[Block]]                             = ???
  override def deploy(deploy: Deploy): F[Long]                                                 = ???
  override def getDeployStatus(hash: Array[Byte]): F[Int]                                      = ???
  override def getDeploysByBlockHash(hash: Array[Byte]): F[Seq[Deploy]]                        = ???
  override def status: F[Status]                                                               = ???
  override def getLatestMessage: F[Block]                                                      = ???
  override def visualizeDag[R](depth: Int, showJustificationLines: Boolean): F[Vector[String]] = ???
}
