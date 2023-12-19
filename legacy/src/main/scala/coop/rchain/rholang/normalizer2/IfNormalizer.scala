package coop.rchain.rholang.normalizer2

import cats.Apply
import cats.syntax.all.*
import io.rhonix.rholang.ast.rholang.Absyn.PIf
import io.rhonix.rholang.{GBoolN, MatchCaseN, MatchN, NilN}

object IfNormalizer {
  def normalizeIf[F[_]: Apply: NormalizerRec](p: PIf): F[MatchN] =
    (NormalizerRec[F].normalize(p.proc_1), NormalizerRec[F].normalize(p.proc_2))
      .mapN((target, trueCaseBody) =>
        MatchN(target, Seq(MatchCaseN(GBoolN(true), trueCaseBody), MatchCaseN(GBoolN(false), NilN))),
      )
}
