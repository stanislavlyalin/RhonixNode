package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.errors.UnrecognizedNormalizerError
import coop.rchain.rholang.normalizer2.env.syntax.all.*
import coop.rchain.rholang.normalizer2.env.{BoundVarWriter, FreeVarReader, FreeVarWriter}
import io.rhonix.rholang.ast.rholang.Absyn.{Case, CaseImpl, PMatch, Proc}
import io.rhonix.rholang.{MatchCaseN, MatchN}

import scala.jdk.CollectionConverters.*

object MatchNormalizer {
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def normalizeMatch[F[_]: Sync: NormalizerRec, T: BoundVarWriter: FreeVarReader: FreeVarWriter](
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
                         patternTuple <- BoundVarWriter[T].withNewBoundVarScope(() =>
                                           FreeVarWriter[T].withNewFreeVarScope(() =>
                                             for {
                                               pattern <- NormalizerRec[F].normalize(pattern)
                                               // Get free variables from the pattern
                                               freeVars = FreeVarReader[T].getFreeVars
                                             } yield (pattern, freeVars),
                                           ),
                                         )

                         (patternResult, freeVars) = patternTuple

                         caseBodyResult <- BoundVarWriter[T].withCopyBoundVarScope { () =>
                                             // Bound free variables in the current scope
                                             BoundVarWriter[T].absorbFree(freeVars)

                                             // Normalize body in the current bound and free variables scope
                                             NormalizerRec[F].normalize(caseBody)
                                           }
                       } yield MatchCaseN(patternResult, caseBodyResult, freeVars.length) +: acc
                     }
    } yield MatchN(targetResult, casesResult.reverse)
  }
}
