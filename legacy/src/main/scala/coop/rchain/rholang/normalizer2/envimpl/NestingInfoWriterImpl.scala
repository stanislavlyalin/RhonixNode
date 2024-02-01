package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.normalizer2.env.NestingInfoWriter

final case class NestingInfoWriterImpl[F[_]](
  private val patternInfo: PatternInfoChain[F],
  private val bundleInfo: BundleInfoChain[F],
) extends NestingInfoWriter[F] {

  override def withinPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R] =
    patternInfo.runWithNewStatus(inReceive)(scopeFn)

  override def withinBundle[R](scopeFn: F[R]): F[R] = bundleInfo.runWithNewStatus(scopeFn)
}
