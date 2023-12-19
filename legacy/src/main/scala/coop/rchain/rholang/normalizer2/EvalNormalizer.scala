package coop.rchain.rholang.normalizer2

import cats.Apply
import io.rhonix.rholang.ParN
import io.rhonix.rholang.ast.rholang.Absyn.PEval

object EvalNormalizer {
  def normalizeEval[F[_]: Apply: NormalizerRec](p: PEval): F[ParN] = NormalizerRec[F].normalize(p.name_)
}
