package coop.rchain.rholang.normalizer2.util

import cats.effect.Sync
import cats.implicits.none
import coop.rchain.rholang.normalizer2.NormalizerRec
import coop.rchain.rholang.normalizer2.util.Mock.*
import coop.rchain.rholang.normalizer2.util.MockNormalizerRec.{mockADT, RemainderADTDefault}
import io.rhonix.rholang.ast.rholang.Absyn.{Name, NameRemainder, Proc, ProcRemainder}
import io.rhonix.rholang.{GStringN, ParN, VarN}

import scala.collection.mutable.ListBuffer

case class MockNormalizerRec[F[_]: Sync, T](
  bWScope: MockBoundVarScope[F],
  fWScope: MockFreeVarScope[F],
  rWriter: MockRestrictWriter[F],
  rReader: MockRestrictReader,
) extends NormalizerRec[F] {
  private val buffer: ListBuffer[TermData] = ListBuffer.empty

  private def addInBuf(term: MockNormalizerRecTerm): Unit =
    buffer.append(
      TermData(
        term = term,
        boundNewScopeLevel = bWScope.getNewScopeLevel,
        boundCopyScopeLevel = bWScope.getCopyScopeLevel,
        freeScopeLevel = fWScope.getScopeLevel,
        insidePattern = rWriter.getInsidePatternFlag,
        insideTopLevelReceive = rWriter.getInsideTopLevelReceivePatternFlag,
        insideBundle = rWriter.getInsideBundleFlag,
      ),
    )

  // Because addInBuf is not pure, we need to use Sync[F].delay

  override def normalize(proc: Proc): F[ParN] = Sync[F].delay {
    addInBuf(ProcTerm(proc))
    mockADT(proc)
  }

  override def normalize(name: Name): F[ParN] = Sync[F].delay {
    addInBuf(NameTerm(name))
    mockADT(name)
  }

  override def normalize(remainder: ProcRemainder): F[Option[VarN]] = Sync[F].delay {
    addInBuf(ProcRemainderTerm(remainder))
    RemainderADTDefault
  }

  override def normalize(remainder: NameRemainder): F[Option[VarN]] = Sync[F].delay {
    addInBuf(NameRemainderTerm(remainder))
    RemainderADTDefault
  }

  def extractData: Seq[TermData] = buffer.toSeq

}

object MockNormalizerRec {
  def mockADT(proc: Proc): ParN         = GStringN(proc.toString)
  def mockADT(name: Name): ParN         = GStringN(name.toString)
  val RemainderADTDefault: Option[VarN] = none
}
