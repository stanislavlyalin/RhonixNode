package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.implicits.{catsSyntaxTuple2Semigroupal, toFunctorOps}
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.interpreter.errors.UnrecognizedNormalizerError
import coop.rchain.rholang.normalizer2.env.*
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

final case class NormalizerRecImpl[F[
  +_,
]: Sync: BoundVarScope: FreeVarScope: NestingInfoWriter, T >: VarSort: BoundVarWriter: BoundVarReader: FreeVarWriter: FreeVarReader]()(
  implicit fWScopeReader: NestingInfoReader,
) extends NormalizerRec[F] {

  implicit val nRec = this

  override def normalize(proc: Proc): F[ParN] = Sync[F].defer {
    def unaryExp(subProc: Proc, constructor: ParN => ExprN): F[ParN] =
      NormalizerRec[F].normalize(subProc).map(constructor)

    def binaryExp(subProcLeft: Proc, subProcRight: Proc, constructor: (ParN, ParN) => ExprN): F[ParN] =
      (NormalizerRec[F].normalize(subProcLeft), NormalizerRec[F].normalize(subProcRight)).mapN(constructor)

    // Create normalizedProc to not explicitly specify the type as F[ParN]
    val normalizedProc = proc match {
      case p: PNegation    => NegationNormalizer.normalizeNegation(p)
      case p: PConjunction => ConjunctionNormalizer.normalizeConjunction(p)
      case p: PDisjunction => DisjunctionNormalizer.normalizeDisjunction(p)
      case p: PSimpleType  => Sync[F].delay(SimpleTypeNormalizer.normalizeSimpleType(p))
      case p: PGround      => GroundNormalizer.normalizeGround(p)
      case p: PCollect     => CollectNormalizer.normalizeCollect(p)
      case p: PVar         => VarNormalizer.normalizeVar[F, T](p)
      case p: PVarRef      => VarRefNormalizer.normalizeVarRef[F, T](p)
      case _: PNil         => Sync[F].delay(NilN: ParN)
      case p: PEval        => EvalNormalizer.normalizeEval(p)
      case p: PMethod      => MethodNormalizer.normalizeMethod(p)

      case p: PNot => unaryExp(p.proc_, ENotN.apply)
      case p: PNeg => unaryExp(p.proc_, ENegN.apply)

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

      case p: PMatches   => MatchesNormalizer.normalizeMatches(p)
      case p: PExprs     => NormalizerRec[F].normalize(p.proc_)
      case p: PSend      => SendNormalizer.normalizeSend(p)
      case p: PSendSynch => SendSynchNormalizer.normalizeSendSynch(p)
      case p: PContr     => ContrNormalizer.normalizeContr(p)
      case p: PInput     => InputNormalizer.normalizeInput(p)
      case p: PPar       => ParNormalizer.normalizePar(p)
      case p: PNew       => NewNormalizer.normalizeNew[F, T](p)
      case p: PBundle    => BundleNormalizer.normalizeBundle(p)
      case p: PLet       => LetNormalizer.normalizeLet(p)
      case p: PMatch     => MatchNormalizer.normalizeMatch(p)
      case p: PIf        => IfNormalizer.normalizeIf(p)
      case p: PIfElse    => IfElseNormalizer.normalizeIfElse(p)

      case _ => Sync[F].raiseError(UnrecognizedNormalizerError("Compilation of construct not yet supported."))
    }
    normalizedProc
  }

  override def normalize(name: Name): F[ParN] = ???

  override def normalize(remainder: ProcRemainder): F[Option[VarN]] = ???

  override def normalize(remainder: NameRemainder): F[Option[VarN]] = ???

}
