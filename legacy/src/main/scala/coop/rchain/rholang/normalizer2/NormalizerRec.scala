package coop.rchain.rholang.normalizer2

import io.rhonix.rholang.ParN
import io.rhonix.rholang.ast.rholang.Absyn.Proc

trait NormalizerRec[F[_]] {
  def normalize(proc: Proc): F[ParN]
}

object NormalizerRec {
  def apply[F[_]](implicit instance: NormalizerRec[F]): NormalizerRec[F] = instance
}
