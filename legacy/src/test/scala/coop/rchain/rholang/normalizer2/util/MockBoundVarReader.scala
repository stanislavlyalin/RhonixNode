package coop.rchain.rholang.normalizer2.util

import coop.rchain.rholang.interpreter.compiler.BoundContext
import coop.rchain.rholang.normalizer2.env.BoundVarReader
import coop.rchain.rholang.normalizer2.util.Mock.{DefPosition, VarReaderData}

case class MockBoundVarReader[T](boundVars: Seq[VarReaderData[T]]) extends BoundVarReader[T] {
  private val boundVarMap: Map[String, BoundContext[T]] =
    boundVars.map(x => (x.name, BoundContext(x.index, x.typ, DefPosition))).toMap

  override def getBoundVar(name: String): Option[BoundContext[T]] = boundVarMap.get(name)

  override def findBoundVar(name: String): Option[(BoundContext[T], Int)] =
    boundVarMap.get(name).map(context => (context, 0)) // Example with level 0

  override def boundVarCount: Int = boundVars.size
}
