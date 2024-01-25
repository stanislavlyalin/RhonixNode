package coop.rchain.rholang.normalizer2

import cats.Apply
import cats.syntax.all.*
import io.rhonix.rholang.ast.rholang.Absyn.PIf
import io.rhonix.rholang.{GBoolN, MatchCaseN, MatchN, NilN}
import sdk.syntax.all.*

object IfNormalizer {
  def normalizeIf[F[_]: Apply: NormalizerRec](p: PIf): F[MatchN] =
    (p.proc_1, p.proc_2)
      .nmap(NormalizerRec[F].normalize)
      .mapN((target, trueCaseBody) =>
        MatchN(target, Seq(MatchCaseN(GBoolN(true), trueCaseBody), MatchCaseN(GBoolN(false), NilN))),
      )
}
