package coop.rchain.rholang.normalizer2

import cats.Apply
import cats.syntax.all.*
import io.rhonix.rholang.ParN
import io.rhonix.rholang.ast.rholang.Absyn.PPar

object ParNormalizer {
  def normalizePar[F[_]: Apply: NormalizerRec](p: PPar): F[ParN] =
    (NormalizerRec[F].normalize(p.proc_1), NormalizerRec[F].normalize(p.proc_2)).mapN(ParN.combine)
}
