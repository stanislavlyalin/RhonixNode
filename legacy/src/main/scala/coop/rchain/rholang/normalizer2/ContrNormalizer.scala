package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.normalizer2.env.{BoundVarScope, BoundVarWriter, FreeVarReader, FreeVarScope}
import coop.rchain.rholang.syntax.*
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

import scala.jdk.CollectionConverters.*

object ContrNormalizer {
  def normalizeContr[F[_]: Sync: NormalizerRec: BoundVarScope: FreeVarScope, T: BoundVarWriter: FreeVarReader](
    p: PContr,
  ): F[ReceiveN] =
    for {
      source <- NormalizerRec[F].normalize(p.name_)

      normalizePattern = for {
                           patterns <- p.listname_.asScala.toList.traverse(NormalizerRec[F].normalize)
                           reminder <- NormalizerRec[F].normalize(p.nameremainder_)
                           vars      = FreeVarReader[T].getFreeVars
                         } yield (ReceiveBindN(patterns, source, reminder, vars.size), vars)
      patternTuple    <- normalizePattern.withNewVarScope(insideReceive = true)

      (bind, freeVars) = patternTuple

      // Normalize body in the current bound and free variables scope
      continuation <- BoundVarScope[F].withCopyBoundVarScope(for {
                        _ <- Sync[F].delay(BoundVarWriter[T].absorbFree(freeVars))
                        r <- NormalizerRec[F].normalize(p.proc_)
                      } yield r)
    } yield ReceiveN(bind, continuation, persistent = true, peek = false, freeVars.size)
}
