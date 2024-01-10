package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.SourcePosition
import coop.rchain.rholang.interpreter.errors.TopLevelLogicalConnectivesNotAllowedError
import coop.rchain.rholang.normalizer2.env.FreeVarScopeReader
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

object ConjunctionNormalizer {
  def normalizeConjunction[F[_]: Sync: NormalizerRec](
    p: PConjunction,
  )(implicit scope: FreeVarScopeReader): F[ConnAndN] =
    if (scope.topLevel) {
      def pos = SourcePosition(p.line_num, p.col_num)
      TopLevelLogicalConnectivesNotAllowedError(s"/\\ (conjunction) at $pos").raiseError
    } else
      (NormalizerRec[F].normalize(p.proc_1), NormalizerRec[F].normalize(p.proc_2))
        .mapN((left, right) => ConnAndN(Seq(left, right)))
}
