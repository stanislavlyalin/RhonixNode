package coop.rchain.rholang.normalizer2.envimpl

import cats.effect.Sync
import coop.rchain.rholang.normalizer2.env.NestingWriter

final case class NestingWriterImpl[F[_]: Sync](
  private val patternInfo: PatternInfoChain,
  private val bundleInfo: BundleInfoChain,
) extends NestingWriter[F] {

  override def withinPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R] =
    patternInfo.runWithNewStatus(inReceive)(scopeFn)

  override def withinBundle[R](scopeFn: F[R]): F[R] = bundleInfo.runWithNewStatus(scopeFn)
}
