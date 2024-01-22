package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.normalizer2.env.*
import coop.rchain.rholang.syntax.normalizerEffectSyntax
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

import scala.jdk.CollectionConverters.*

object ContrNormalizer {
  def normalizeContr[F[
    _,
  ]: Sync: NormalizerRec: BoundVarScope: FreeVarScope: RestrictWriter, T: BoundVarWriter: FreeVarReader](
    p: PContr,
  ): F[ReceiveN] =
    for {
      source <- NormalizerRec[F].normalize(p.name_)

      normalizePattern = for {
                           patterns <- p.listname_.asScala.toList.traverse(NormalizerRec[F].normalize)
                           reminder <- NormalizerRec[F].normalize(p.nameremainder_)
                           vars      = FreeVarReader[T].getFreeVars
                         } yield (ReceiveBindN(patterns, source, reminder, vars.size), vars)
      patternTuple    <- normalizePattern.asPattern(inReceive = true)

      (bind, freeVars) = patternTuple

      // Normalize body in the current bound and free variables scope
      continuation <- NormalizerRec[F].normalize(p.proc_).withAbsorbedFreeVars(freeVars)

    } yield ReceiveN(bind, continuation, persistent = true, peek = false, freeVars.size)
}
