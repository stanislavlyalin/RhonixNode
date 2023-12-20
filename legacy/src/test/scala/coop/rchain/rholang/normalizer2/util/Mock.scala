package coop.rchain.rholang.normalizer2.util

import cats.Applicative
import coop.rchain.rholang.interpreter.compiler.SourcePosition
import io.rhonix.rholang.ast.rholang.Absyn.{Name, NameRemainder, Proc, ProcRemainder}

object Mock {

  case class BoundVarWriterData[T](name: String, varType: T, newScopeLevel: Int = 0, copyScopeLevel: Int = 0)
  case class FreeVarWriterData[T](name: String, varType: T, scopeLevel: Int = 0)
  case class VarReaderData[T](name: String, index: Int, typ: T)
  case class TermData(
    term: MockNormalizerRecTerm,
    boundNewScopeLevel: Int = 0,
    boundCopyScopeLevel: Int = 0,
    freeScopeLevel: Int = 0,
  )

  sealed trait MockNormalizerRecTerm
  final case class ProcTerm(proc: Proc)                            extends MockNormalizerRecTerm
  final case class NameTerm(name: Name)                            extends MockNormalizerRecTerm
  final case class ProcRemainderTerm(remainder: ProcRemainder)     extends MockNormalizerRecTerm
  final case class NameRemainderTerm(nameRemainder: NameRemainder) extends MockNormalizerRecTerm

  val DefPosition: SourcePosition = SourcePosition(0, 0)
  val DefFreeVarIndex: Int        = 0

  def createMockDSL[F[_]: Applicative, T](
    initBoundVars: Seq[VarReaderData[T]] = Seq(),
    initFreeVars: Seq[VarReaderData[T]] = Seq(),
    isTopLevel: Boolean = true,
    isReceivePattern: Boolean = false,
  ): (
    MockNormalizerRec[F, T],
    MockBoundVarWriter[T],
    MockBoundVarReader[T],
    MockFreeVarWriter[T],
    MockFreeVarReader[T],
  ) = {
    val mockBVW: MockBoundVarWriter[T] = MockBoundVarWriter[T]()
    val mockBVR: MockBoundVarReader[T] = MockBoundVarReader[T](initBoundVars)

    val mockFVW: MockFreeVarWriter[T] = MockFreeVarWriter[T]()
    val mockFVR: MockFreeVarReader[T] = MockFreeVarReader[T](initFreeVars, isTopLevel, isReceivePattern)

    val mockNormalizerRec: MockNormalizerRec[F, T] = MockNormalizerRec[F, T](mockBVW, mockFVW)

    (mockNormalizerRec, mockBVW, mockBVR, mockFVW, mockFVR)
  }
}
