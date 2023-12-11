package coop.rchain.rholang.normalizer2

import io.rhonix.rholang.ParN
import io.rhonix.rholang.ast.rholang.Absyn.{Name, Proc}

trait NormalizerRec[F[_]] {
  def normalize(proc: Proc): F[ParN]

  def normalize(proc: Name): F[ParN]
}

object NormalizerRec {
  def apply[F[_]](implicit instance: NormalizerRec[F]): instance.type = instance
}
