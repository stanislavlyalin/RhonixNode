package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.*
import coop.rchain.rholang.interpreter.errors.*
import coop.rchain.rholang.normalizer2.env.*
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

final case class NormalizerRecImpl[
  F[+_]: Sync: BoundVarScope: FreeVarScope: NestingInfoWriter,
  T >: VarSort: BoundVarWriter: BoundVarReader: FreeVarWriter: FreeVarReader,
]()(implicit nestingInfo: NestingInfoReader)
    extends NormalizerRec[F] {

  implicit val nRec: NormalizerRec[F] = this

  override def normalize(proc: Proc): F[ParN] = NormalizerRecImpl.normalize[F, T](proc)

  override def normalize(name: Name): F[ParN] = name match {
    case nv: NameVar      => VarNormalizer.asBoundVar[F, T](nv.var_, SourcePosition(nv.line_num, nv.col_num), NameSort)
    case nq: NameQuote    => NormalizerRec[F].normalize(nq.proc_)
    case wc: NameWildcard => VarNormalizer.asWildcard[F](SourcePosition(wc.line_num, wc.col_num))
  }

  override def normalize(remainder: ProcRemainder): F[Option[VarN]] = remainder match {
    case _: ProcRemainderEmpty => none.pure
    case pr: ProcRemainderVar  => VarNormalizer.asRemainder[F, T](pr.procvar_).map(_.some)
  }

  override def normalize(remainder: NameRemainder): F[Option[VarN]] = remainder match {
    case _: NameRemainderEmpty => none.pure
    case nr: NameRemainderVar  => VarNormalizer.asRemainder[F, T](nr.procvar_).map(_.some)
  }
}

object NormalizerRecImpl {

  /** Normalizes parser AST types to core Rholang AST types
   *
   * @param proc input parser AST object
   * @return core Rholang AST object [[ParN]]
   */
  def normalize[
    F[+_]: Sync: NormalizerRec: BoundVarScope: FreeVarScope: NestingInfoWriter,
    T >: VarSort: BoundVarWriter: BoundVarReader: FreeVarWriter: FreeVarReader,
  ](proc: Proc)(implicit nestingInfo: NestingInfoReader): F[ParN] = {

    def unaryExp(subProc: Proc, constructor: ParN => ExprN): F[ParN] =
      NormalizerRec[F].normalize(subProc).map(constructor)

    def binaryExp(subProcLeft: Proc, subProcRight: Proc, constructor: (ParN, ParN) => ExprN): F[ParN] =
      (NormalizerRec[F].normalize(subProcLeft), NormalizerRec[F].normalize(subProcRight)).mapN(constructor)

    // Dispatch to normalizer methods depending on parser AST type
    proc match {
      /* Terminal expressions (0-arity constructors) */
      /* =========================================== */
      case _: PNil        => (NilN: ParN).pure
      case p: PGround     => GroundNormalizer.normalizeGround[F](p)
      case p: PVar        => VarNormalizer.normalizeProcVar[F, T](p)
      case p: PVarRef     => VarRefNormalizer.normalizeVarRef[F, T](p)
      case p: PSimpleType => Sync[F].delay(SimpleTypeNormalizer.normalizeSimpleType(p))

      /* Unary expressions (1-arity constructors) */
      /* ======================================== */
      case p: PEval     => EvalNormalizer.normalizeEval[F](p)
      case p: PBundle   => BundleNormalizer.normalizeBundle[F](p)
      case p: PNegation => NegationNormalizer.normalizeNegation[F](p)
      case p: PExprs    => NormalizerRec[F].normalize(p.proc_)
      case p: PNot      => unaryExp(p.proc_, ENotN.apply)
      case p: PNeg      => unaryExp(p.proc_, ENegN.apply)

      /* Binary expressions (2-arity constructors) */
      /* ========================================= */
      case p: PPar            => ParNormalizer.normalizePar[F](p)
      case p: PMatches        => MatchesNormalizer.normalizeMatches[F](p)
      case p: PConjunction    => ConjunctionNormalizer.normalizeConjunction[F](p)
      case p: PDisjunction    => DisjunctionNormalizer.normalizeDisjunction[F](p)
      case p: PMult           => binaryExp(p.proc_1, p.proc_2, EMultN.apply)
      case p: PDiv            => binaryExp(p.proc_1, p.proc_2, EDivN.apply)
      case p: PMod            => binaryExp(p.proc_1, p.proc_2, EModN.apply)
      case p: PPercentPercent => binaryExp(p.proc_1, p.proc_2, EPercentPercentN.apply)
      case p: PAdd            => binaryExp(p.proc_1, p.proc_2, EPlusN.apply)
      case p: PMinus          => binaryExp(p.proc_1, p.proc_2, EMinusN.apply)
      case p: PPlusPlus       => binaryExp(p.proc_1, p.proc_2, EPlusPlusN.apply)
      case p: PMinusMinus     => binaryExp(p.proc_1, p.proc_2, EMinusMinusN.apply)
      case p: PLt             => binaryExp(p.proc_1, p.proc_2, ELtN.apply)
      case p: PLte            => binaryExp(p.proc_1, p.proc_2, ELteN.apply)
      case p: PGt             => binaryExp(p.proc_1, p.proc_2, EGtN.apply)
      case p: PGte            => binaryExp(p.proc_1, p.proc_2, EGteN.apply)
      case p: PEq             => binaryExp(p.proc_1, p.proc_2, EEqN.apply)
      case p: PNeq            => binaryExp(p.proc_1, p.proc_2, ENeqN.apply)
      case p: PAnd            => binaryExp(p.proc_1, p.proc_2, EAndN.apply)
      case p: POr             => binaryExp(p.proc_1, p.proc_2, EOrN.apply)
      case p: PShortAnd       => binaryExp(p.proc_1, p.proc_2, EShortAndN.apply)
      case p: PShortOr        => binaryExp(p.proc_1, p.proc_2, EShortOrN.apply)

      /* N-ary parameter expressions (N-arity constructors) */
      /* ================================================== */
      case p: PCollect   => CollectNormalizer.normalizeCollect[F](p)
      case p: PSend      => SendNormalizer.normalizeSend[F](p)
      case p: PSendSynch => SendSynchNormalizer.normalizeSendSynch[F](p)
      case p: PContr     => ContrNormalizer.normalizeContr[F, T](p)
      case p: PInput     => InputNormalizer.normalizeInput(p)
      case p: PNew       => NewNormalizer.normalizeNew[F, T](p)
      case p: PLet       => LetNormalizer.normalizeLet[F, T](p)
      case p: PMatch     => MatchNormalizer.normalizeMatch[F, T](p)
      case p: PIf        => IfNormalizer.normalizeIf[F](p)
      case p: PIfElse    => IfElseNormalizer.normalizeIfElse[F](p)
      case p: PMethod    => MethodNormalizer.normalizeMethod[F](p)

      case _ => UnrecognizedNormalizerError("Compilation of construct not yet supported.").raiseError
    }
  }
}
