package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.normalizer2.env.RestrictReader

case class MockRestrictReader(
  insidePatternInit: Boolean,
  insideTopLevelReceivePatternInit: Boolean,
  insideBundleInit: Boolean,
) extends RestrictReader {
  override def insidePattern: Boolean                = insidePatternInit
  override def insideTopLevelReceivePattern: Boolean = insideTopLevelReceivePatternInit
  override def insideBundle: Boolean                 = insideBundleInit
}
