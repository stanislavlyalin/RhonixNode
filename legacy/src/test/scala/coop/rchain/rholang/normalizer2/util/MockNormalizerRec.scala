package coop.rchain.rholang.normalizer2.util

import cats.Applicative
import cats.implicits.{catsSyntaxApplicativeId, none}
import coop.rchain.rholang.normalizer2.NormalizerRec
import coop.rchain.rholang.normalizer2.util.Mock.*
import io.rhonix.rholang.{NilN, ParN, VarN}
import io.rhonix.rholang.ast.rholang.Absyn.{Name, NameRemainder, Proc, ProcRemainder}

import scala.collection.mutable.ListBuffer

case class MockNormalizerRec[F[_]: Applicative, T](mockBVW: MockBoundVarWriter[T], mockFVW: MockFreeVarWriter[T])
    extends NormalizerRec[F] {
  private val buffer: ListBuffer[TermData] = ListBuffer.empty

  private def addInBuf(term: MockNormalizerRecTerm): Unit =
    buffer.append(
      TermData(
        term = term,
        boundNewScopeLevel = mockBVW.newScopeLevel(),
        boundCopyScopeLevel = mockBVW.copyScopeLevel(),
        freeScopeLevel = mockFVW.newScopeLevel(),
      ),
    )

  override def normalize(proc: Proc): F[ParN] = {
    addInBuf(ProcTerm(proc))
    (NilN: ParN).pure
  }

  override def normalize(name: Name): F[ParN] = {
    addInBuf(NameTerm(name))
    (NilN: ParN).pure
  }

  override def normalize(remainder: ProcRemainder): F[Option[VarN]] = {
    addInBuf(ProcRemainderTerm(remainder))
    none[VarN].pure
  }

  override def normalize(remainder: NameRemainder): F[Option[VarN]] = {
    addInBuf(NameRemainderTerm(remainder))
    none[VarN].pure
  }

  def extractData: Seq[TermData] = buffer.toSeq
}
