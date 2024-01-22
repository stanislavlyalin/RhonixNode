package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.interpreter.errors.TopLevelLogicalConnectivesNotAllowedError
import coop.rchain.rholang.normalizer2.env.RestrictReader
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

object ConjunctionNormalizer {
  def normalizeConjunction[F[_]: Sync: NormalizerRec](
    p: PConjunction,
  )(implicit restrict: RestrictReader): F[ConnAndN] =
    if (restrict.insidePattern)
      (NormalizerRec[F].normalize(p.proc_1), NormalizerRec[F].normalize(p.proc_2))
        .mapN((left, right) => ConnAndN(Seq(left, right)))
    else {
      def pos = SourcePosition(p.line_num, p.col_num)
      TopLevelLogicalConnectivesNotAllowedError(s"/\\ (conjunction) at $pos").raiseError
    }
}
