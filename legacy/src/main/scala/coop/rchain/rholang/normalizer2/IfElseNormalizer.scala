package coop.rchain.rholang.normalizer2

import cats.Apply
import cats.syntax.all.*
import io.rhonix.rholang.ast.rholang.Absyn.PIfElse
import io.rhonix.rholang.{GBoolN, MatchCaseN, MatchN}

object IfElseNormalizer {
  def normalizeIfElse[F[_]: Apply: NormalizerRec](p: PIfElse): F[MatchN] =
    (NormalizerRec[F].normalize(p.proc_1), NormalizerRec[F].normalize(p.proc_2), NormalizerRec[F].normalize(p.proc_3))
      .mapN((target, trueCaseBody, falseCaseBody) =>
        MatchN(target, Seq(MatchCaseN(GBoolN(true), trueCaseBody), MatchCaseN(GBoolN(false), falseCaseBody))),
      )
}
