package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.*
import coop.rchain.rholang.interpreter.errors.{
  TopLevelFreeVariablesNotAllowedError,
  TopLevelWildcardsNotAllowedError,
  UnexpectedBundleContent,
  UnexpectedProcContext,
  UnexpectedReuseOfProcContextFree,
}
import coop.rchain.rholang.normalizer2.env.{BoundVarReader, FreeVarReader, FreeVarWriter, RestrictReader}
import io.rhonix.rholang.ast.rholang.Absyn.{PVar, ProcVarVar, ProcVarWildcard}
import io.rhonix.rholang.{BoundVarN, FreeVarN, VarN, WildcardN}

object VarNormalizer {
  def normalizeVar[F[_]: Sync, T >: VarSort: BoundVarReader: FreeVarReader: FreeVarWriter](
    p: PVar,
  )(implicit restrict: RestrictReader): F[VarN] =
    Sync[F].defer {
      def pos = SourcePosition(p.line_num, p.col_num)
      p.procvar_ match {
        case pvv: ProcVarVar =>
          BoundVarReader[T].getBoundVar(pvv.var_) match {
            case Some(BoundContext(level, ProcSort, _)) =>
              (BoundVarN(level): VarN).pure[F]

            case Some(BoundContext(_, _, sourcePosition)) =>
              UnexpectedProcContext(pvv.var_, sourcePosition, pos).raiseError

            case None =>
              if (restrict.insidePattern)
                if (restrict.insideBundle)
                  UnexpectedBundleContent(s"Illegal free variable in bundle at $pos").raiseError
                else
                  FreeVarReader[T].getFreeVar(pvv.var_) match {
                    case None =>
                      val index = FreeVarWriter[T].putFreeVar(pvv.var_, ProcSort, pos)
                      (FreeVarN(index): VarN).pure[F]

                    case Some(FreeContext(_, _, firstSourcePosition)) =>
                      UnexpectedReuseOfProcContextFree(pvv.var_, firstSourcePosition, pos).raiseError
                  }
              else TopLevelFreeVariablesNotAllowedError(s"${pvv.var_} at $pos").raiseError
          }

        case _: ProcVarWildcard =>
          if (restrict.insidePattern)
            if (restrict.insideBundle) UnexpectedBundleContent(s"Illegal wildcard in bundle at $pos").raiseError
            else (WildcardN: VarN).pure[F]
          else TopLevelWildcardsNotAllowedError(s"_ (wildcard) at $pos").raiseError
      }
    }
}
