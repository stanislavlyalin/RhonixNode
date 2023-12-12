package coop.rchain.rholang.normalizer2

import io.rhonix.rholang.ParN
import io.rhonix.rholang.ast.rholang.Absyn.{Name, NameRemainder, Proc, ProcRemainder}

trait NormalizerRec[F[_]] {
  def normalize(proc: Proc): F[ParN]

  def normalize(proc: Name): F[ParN]

  // TODO: Remove when reminder will be replaced with more general spread operator.

  def normalize(proc: ProcRemainder): F[ParN]

  def normalize(proc: NameRemainder): F[ParN]
}

object NormalizerRec {
  def apply[F[_]](implicit instance: NormalizerRec[F]): instance.type = instance
}
