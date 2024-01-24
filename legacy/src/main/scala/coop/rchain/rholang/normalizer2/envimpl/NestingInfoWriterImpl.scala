package coop.rchain.rholang.normalizer2.envimpl

import cats.effect.Sync
import coop.rchain.rholang.normalizer2.env.NestingInfoWriter
import coop.rchain.rholang.syntax.*

final class NestingInfoWriterImpl[F[_]: Sync](
  private val patternInfo: HistoryChain[(Boolean, Boolean)],
  private val bundleInfo: HistoryChain[Boolean],
) extends NestingInfoWriter[F] {

  override def withinPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R] =
    patternInfo.runWithNewDataInChain(scopeFn, (true, inReceive))

  override def withinBundle[R](scopeFn: F[R]): F[R] = bundleInfo.runWithNewDataInChain(scopeFn, true)
}

object NestingInfoWriterImpl {
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def apply[F[_]: Sync](
    patternInfo: HistoryChain[(Boolean, Boolean)],
    bundleInfo: HistoryChain[Boolean],
  ): NestingInfoWriterImpl[F] = {
    // Initialize with the default values as false
    patternInfo.push((false, false))
    bundleInfo.push(false)
    new NestingInfoWriterImpl(patternInfo, bundleInfo)
  }
}
