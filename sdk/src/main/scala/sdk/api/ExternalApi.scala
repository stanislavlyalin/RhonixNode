package sdk.api

import sdk.api.data.*

trait ExternalApi[F[_]] {
  // Block API
  def getBlockByHash(hash: Array[Byte]): F[Option[Block]]

  // Deploy API
  def deploy(deploy: Deploy): F[Long]
  def getDeployStatus(hash: Array[Byte]): F[Int]
  def getDeploysByBlockHash(hash: Array[Byte]): F[Seq[Deploy]]

  // Diagnostic
  def status: F[Status]
  def getLatestMessage: F[Block]
  def visualizeDag[R](depth: Int, showJustificationLines: Boolean): F[Vector[String]]
}
