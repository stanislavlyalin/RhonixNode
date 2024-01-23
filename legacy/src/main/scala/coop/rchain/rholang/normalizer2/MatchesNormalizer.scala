package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.normalizer2.env.{BoundVarScope, FreeVarScope, NestingInfoWriter}
import coop.rchain.rholang.syntax.*
import io.rhonix.rholang.EMatchesN
import io.rhonix.rholang.ast.rholang.Absyn.{PMatches, Proc}

object MatchesNormalizer {
  def normalizeMatches[F[_]: Sync: NormalizerRec: BoundVarScope: FreeVarScope: NestingInfoWriter](
    p: PMatches,
  ): F[EMatchesN] = {
    // The expression "target matches pattern" should have the same semantics as "match target { pattern => true ; _ => false}".
    // Therefore, there is no need to bind free variables in the pattern because the case body will always be true.
    def normalizePattern(proc: Proc) = NormalizerRec[F].normalize(proc).asPattern()

    (NormalizerRec[F].normalize(p.proc_1), normalizePattern(p.proc_2)).mapN(EMatchesN.apply)
  }
}
