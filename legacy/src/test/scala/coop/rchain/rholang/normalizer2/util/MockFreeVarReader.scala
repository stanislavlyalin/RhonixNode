package coop.rchain.rholang.normalizer2.util
import coop.rchain.rholang.interpreter.compiler.FreeContext
import coop.rchain.rholang.normalizer2.env.FreeVarReader
import coop.rchain.rholang.normalizer2.util.Mock.DefPosition

case class MockFreeVarReader[T](
  freeVars: Map[String, (Int, T)],
) extends FreeVarReader[T] {

  private val freeVarMap: Map[String, FreeContext[T]] =
    freeVars.map { case (name, (index, varType)) => name -> FreeContext(index, varType, DefPosition) }

  override def getFreeVars: Seq[(String, FreeContext[T])]       = freeVarMap.toSeq
  override def getFreeVar(name: String): Option[FreeContext[T]] = freeVarMap.get(name)
}
