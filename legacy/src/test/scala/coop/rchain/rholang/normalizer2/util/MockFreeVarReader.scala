package coop.rchain.rholang.normalizer2.util
import coop.rchain.rholang.interpreter.compiler.FreeContext
import coop.rchain.rholang.normalizer2.env.FreeVarReader
import coop.rchain.rholang.normalizer2.util.Mock.{DefPosition, VarReaderData}

case class MockFreeVarReader[T](
  freeVars: Seq[VarReaderData[T]],
  isTopLevel: Boolean,
  isReceivePattern: Boolean,
) extends FreeVarReader[T] {

  private val freeVarMap: Map[String, FreeContext[T]] =
    freeVars.map(x => (x.name, FreeContext(x.index, x.typ, DefPosition))).toMap

  override def getFreeVars: Seq[(String, FreeContext[T])]       = freeVarMap.toSeq
  override def getFreeVar(name: String): Option[FreeContext[T]] = freeVarMap.get(name)
  override def topLevel: Boolean                                = isTopLevel
  override def topLevelReceivePattern: Boolean                  = isReceivePattern && isTopLevel
}
