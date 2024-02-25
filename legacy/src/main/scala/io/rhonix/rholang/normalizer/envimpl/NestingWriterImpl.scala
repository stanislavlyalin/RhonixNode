package io.rhonix.rholang.normalizer.envimpl

import cats.effect.Sync
import io.rhonix.rholang.normalizer.env.NestingWriter
import io.rhonix.rholang.normalizer.syntax.all.*

final case class NestingWriterImpl[F[_]: Sync](
  patternInfo: HistoryChain[(Boolean, Boolean)],
  bundleInfo: HistoryChain[Boolean],
) extends NestingWriter[F] {
  override def withinPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R] =
    patternInfo.runWithNewDataInChain(scopeFn, (true, inReceive))

  override def withinBundle[R](scopeFn: F[R]): F[R] =
    bundleInfo.runWithNewDataInChain(scopeFn, true)
}
