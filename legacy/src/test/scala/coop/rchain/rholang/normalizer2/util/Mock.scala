package coop.rchain.rholang.normalizer2.util

import cats.Applicative
import cats.effect.Sync
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

  // (nRec, bVScope, bVW, bVR, fVScope, fVW, fVR, fVScopeReader)

  /**
   * Create a mock DSL for testing the normalizer.
   * @param initBoundVars initial bound variables for the mock bound variable reader.
   *                      The key is the name of the variable. The value is a tuple of the index and the type.
   * @param initFreeVars initial free variables for the mock free variable reader.
   *                      The key is the name of the variable. The value is a tuple of the index and the type.
   * @param isTopLevel initial value for the isTopLevel flag of the mock free variable scope reader.
   * @param isReceivePattern initial value for the isReceivePattern flag of the mock free variable scope reader.
   * @return all the components of the mock DSL.
   */
  def createMockDSL[F[_]: Sync, T](
    initBoundVars: Map[String, (Int, T)] = Map[String, (Int, T)](),
    initFreeVars: Map[String, (Int, T)] = Map[String, (Int, T)](),
    isTopLevel: Boolean = true,
    isReceivePattern: Boolean = false,
  ): (
    MockNormalizerRec[F, T],
    MockBoundVarScope[F],
    MockBoundVarWriter[F, T],
    MockBoundVarReader[T],
    MockFreeVarScope[F],
    MockFreeVarWriter[F, T],
    MockFreeVarReader[T],
    MockFreeVarScopeReader,
  ) = {
    val mockBVScope: MockBoundVarScope[F] = MockBoundVarScope[F]()
    val mockBVW: MockBoundVarWriter[F, T] = MockBoundVarWriter[F, T](mockBVScope)
    val mockBVR: MockBoundVarReader[T]    = MockBoundVarReader[T](initBoundVars)

    val mockFVScope: MockFreeVarScope[F] = MockFreeVarScope[F]()
    val mockFVW: MockFreeVarWriter[F, T] = MockFreeVarWriter[F, T](mockFVScope)
    val mockFVR: MockFreeVarReader[T]    = MockFreeVarReader[T](initFreeVars)

    val mockNormalizerRec: MockNormalizerRec[F, T] = MockNormalizerRec[F, T](mockBVScope, mockFVScope)

    val mockFVScopeReader: MockFreeVarScopeReader = MockFreeVarScopeReader(isTopLevel, isReceivePattern)
    (mockNormalizerRec, mockBVScope, mockBVW, mockBVR, mockFVScope, mockFVW, mockFVR, mockFVScopeReader)
  }
}
