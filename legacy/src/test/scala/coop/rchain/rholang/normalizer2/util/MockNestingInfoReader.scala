package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.normalizer2.env.NestingInfoReader

case class MockNestingInfoReader(
  insidePatternInit: Boolean,
  insideTopLevelReceivePatternInit: Boolean,
  insideBundleInit: Boolean,
) extends NestingInfoReader {
  override def insidePattern: Boolean                = insidePatternInit
  override def insideTopLevelReceivePattern: Boolean = insideTopLevelReceivePatternInit
  override def insideBundle: Boolean                 = insideBundleInit
}
