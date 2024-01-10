package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.normalizer2.env.FreeVarScopeReader

case class MockFreeVarScopeReader(
  isTopLevel: Boolean,
  isReceivePattern: Boolean,
) extends FreeVarScopeReader {
  override def topLevel: Boolean               = isTopLevel
  override def topLevelReceivePattern: Boolean = isReceivePattern && isTopLevel
}
