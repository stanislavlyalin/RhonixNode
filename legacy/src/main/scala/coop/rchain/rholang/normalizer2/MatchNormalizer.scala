package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.errors.UnrecognizedNormalizerError
import coop.rchain.rholang.normalizer2.env.{BoundVarScope, BoundVarWriter, FreeVarReader, FreeVarScope}
import coop.rchain.rholang.syntax.*
import io.rhonix.rholang.ast.rholang.Absyn.{Case, CaseImpl, PMatch}
import io.rhonix.rholang.{MatchCaseN, MatchN}

import scala.jdk.CollectionConverters.*

object MatchNormalizer {
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def normalizeMatch[F[_]: Sync: NormalizerRec: BoundVarScope: FreeVarScope, T: BoundVarWriter: FreeVarReader](
    p: PMatch,
  ): F[MatchN] = {
    def normalizeCase(c: Case): F[MatchCaseN] = c match {
      case ci: CaseImpl =>
        val (pattern, caseBody) = (ci.proc_1, ci.proc_2)
        for {
          // Normalize pattern in a fresh bound and free variables scope
          patternTuple <- BoundVarScope[F].withNewVarScope()(for {
                            pattern <- NormalizerRec[F].normalize(pattern)
                            // Get free variables from the pattern
                            freeVars = FreeVarReader[T].getFreeVars
                          } yield (pattern, freeVars))

          (patternResult, freeVars) = patternTuple

          caseBodyResult <- BoundVarScope[F].withCopyBoundVarScope(for {
                              _ <- Sync[F].delay(BoundVarWriter[T].absorbFree(freeVars))
                              r <- NormalizerRec[F].normalize(caseBody)
                            } yield r)

        } yield MatchCaseN(patternResult, caseBodyResult, freeVars.length)

      case _ => UnrecognizedNormalizerError("Unexpected Case implementation.").raiseError
    }

    (NormalizerRec[F].normalize(p.proc_), p.listcase_.asScala.toVector.traverse(normalizeCase)).mapN(MatchN.apply)
  }
}
