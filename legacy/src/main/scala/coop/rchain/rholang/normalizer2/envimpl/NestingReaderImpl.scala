package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.normalizer2.env.NestingReader

final class NestingReaderImpl(
  private val insidePatternStatusFn: () => Boolean,
  private val insideTopLevelReceivePatternStatusFn: () => Boolean,
  private val insideBundleStatusFn: () => Boolean,
) extends NestingReader {

  override def insidePattern: Boolean = insidePatternStatusFn()

  override def insideTopLevelReceivePattern: Boolean = insideTopLevelReceivePatternStatusFn()

  override def insideBundle: Boolean = insideBundleStatusFn()
}

object NestingReaderImpl {
  def apply(
    insidePatternFn: () => Boolean,
    insideTopLevelReceivePatternFn: () => Boolean,
    insideBundleFn: () => Boolean,
  ): NestingReaderImpl =
    new NestingReaderImpl(insidePatternFn, insideTopLevelReceivePatternFn, insideBundleFn)
}
