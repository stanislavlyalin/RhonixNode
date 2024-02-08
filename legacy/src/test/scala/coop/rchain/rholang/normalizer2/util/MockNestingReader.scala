package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.normalizer2.env.NestingReader

case class MockNestingReader(
  insidePatternInit: Boolean,
  insideTopLevelReceivePatternInit: Boolean,
  insideBundleInit: Boolean,
) extends NestingReader {
  override def insidePattern: Boolean                = insidePatternInit
  override def insideTopLevelReceivePattern: Boolean = insideTopLevelReceivePatternInit
  override def insideBundle: Boolean                 = insideBundleInit
}
