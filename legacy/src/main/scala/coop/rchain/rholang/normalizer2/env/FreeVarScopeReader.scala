package coop.rchain.rholang.normalizer2.env

trait FreeVarScopeReader {

  /** Flag if free variable scope is on top level, meaning not within the pattern */
  def topLevel: Boolean

  /** Flag if free variable scope in the receive pattern and this receive is not inside any other pattern */
  def topLevelReceivePattern: Boolean
}
