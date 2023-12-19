package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.errors.UnrecognizedNormalizerError
import coop.rchain.rholang.normalizer2.env.syntax.all.*
import coop.rchain.rholang.normalizer2.env.{BoundVarWriter, FreeVarReader, FreeVarWriter}
import io.rhonix.rholang.ast.rholang.Absyn.{Case, CaseImpl, PMatch}
import io.rhonix.rholang.{MatchCaseN, MatchN}

import scala.jdk.CollectionConverters.*

object MatchNormalizer {
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def normalizeMatch[F[_]: Sync: NormalizerRec, T: BoundVarWriter: FreeVarReader: FreeVarWriter](
    p: PMatch,
  ): F[MatchN] = {
    def normalizeCase(c: Case): F[MatchCaseN] = c match {
      case ci: CaseImpl =>
        val (pattern, caseBody) = (ci.proc_1, ci.proc_2)
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
                              BoundVarWriter[T].absorbFree(freeVars)
                              NormalizerRec[F].normalize(caseBody)
                            }
        } yield MatchCaseN(patternResult, caseBodyResult, freeVars.length)
      case _            => UnrecognizedNormalizerError("Unexpected Case implementation.").raiseError
    }

    (NormalizerRec[F].normalize(p.proc_), p.listcase_.asScala.toList.traverse(normalizeCase)).mapN(MatchN.apply)
  }
}
