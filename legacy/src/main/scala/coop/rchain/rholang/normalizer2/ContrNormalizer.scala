package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.normalizer2.env.{BoundVarScope, BoundVarWriter, FreeVarReader, FreeVarScope, FreeVarWriter}
import coop.rchain.rholang.syntax.*
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

import scala.jdk.CollectionConverters.*

object ContrNormalizer {
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def normalizeContr[F[_]: Sync: NormalizerRec: BoundVarScope: FreeVarScope, T: BoundVarWriter: FreeVarReader](
    p: PContr,
  ): F[ReceiveN] =
    for {
      source <- NormalizerRec[F].normalize(p.name_)

      patternTuple <- BoundVarScope[F].withNewVarScope(insideReceive = true)(Sync[F].defer {
                        for {
                          patterns <- p.listname_.asScala.toList.traverse(NormalizerRec[F].normalize)
                          reminder <- NormalizerRec[F].normalize(p.nameremainder_)
                          vars      = FreeVarReader[T].getFreeVars
                        } yield (ReceiveBindN(patterns, source, reminder, vars.size), vars)
                      })

      (bind, freeVars) = patternTuple

      // Normalize body in the current bound and free variables scope
      continuation <- BoundVarScope[F].withCopyBoundVarScope(Sync[F].defer {
                        BoundVarWriter[T].absorbFree(freeVars)
                        NormalizerRec[F].normalize(p.proc_)
                      })
    } yield ReceiveN(bind, continuation, persistent = true, peek = false, freeVars.size)
}
