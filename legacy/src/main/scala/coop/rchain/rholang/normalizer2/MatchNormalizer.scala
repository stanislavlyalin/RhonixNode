package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.errors.UnrecognizedNormalizerError
import coop.rchain.rholang.normalizer2.env.{BoundVarWriter, FreeVarReader, FreeVarWriter}
import io.rhonix.rholang.ast.rholang.Absyn.{Case, CaseImpl, PMatch, Proc}
import io.rhonix.rholang.{MatchCaseN, MatchN}

import scala.jdk.CollectionConverters.*

object MatchNormalizer {
  def normalizerMatch[F[_]: Sync: NormalizerRec, T: BoundVarWriter: FreeVarReader: FreeVarWriter](
    p: PMatch,
  ): F[MatchN] = {
    def liftCase(c: Case): F[(Proc, Proc)] = c match {
      case ci: CaseImpl => (ci.proc_1, ci.proc_2).pure
      case _            => UnrecognizedNormalizerError("Unexpected Case implementation.").raiseError
    }

    for {
      targetResult <- NormalizerRec[F].normalize(p.proc_)

      cases <- p.listcase_.asScala.toList.traverse(liftCase)

      initAcc      = Vector[MatchCaseN]()
      casesResult <- cases.foldM(initAcc) { case (acc, (pattern, caseBody)) =>
                       for {
                         // Normalize pattern in a fresh bound and free variables scope
                         // TODO: extract two nested `with...` to `withNewVarScope` extension method
                         patternTuple <- BoundVarWriter[T].withNewBoundVarScope(() =>
                                           FreeVarWriter[T].withNewFreeVarScope(() =>
                                             for {
                                               pattern     <- NormalizerRec[F].normalize(pattern)
                                               // Get free variables from the pattern
                                               freeVars     = FreeVarReader[T].getFreeVars
                                               freeVarCount = FreeVarReader[T].countNoWildcards
                                             } yield (pattern, freeVars, freeVarCount),
                                           ),
                                         )

                         (patternResult, freeVars, freeVarCount) = patternTuple

                         // Bound free variables in the current scope
                         _ = BoundVarWriter[T].absorbFree(freeVars)

                         // Normalize body in the current bound and free variables scope
                         caseBodyResult <- NormalizerRec[F].normalize(caseBody)
                       } yield MatchCaseN(patternResult, caseBodyResult, freeVarCount) +: acc
                     }
    } yield MatchN(targetResult, casesResult.reverse)
  }
}
