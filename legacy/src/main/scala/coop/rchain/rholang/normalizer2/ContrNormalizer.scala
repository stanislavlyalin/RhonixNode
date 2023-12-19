package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.normalizer2.env.syntax.all.*
import coop.rchain.rholang.normalizer2.env.{BoundVarWriter, FreeVarReader, FreeVarWriter}
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

import scala.jdk.CollectionConverters.*

object ContrNormalizer {
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def normalizeInput[F[_]: Sync: NormalizerRec, T: BoundVarWriter: FreeVarReader: FreeVarWriter](
    p: PContr,
  ): F[ReceiveN] =
    for {
      source <- NormalizerRec[F].normalize(p.name_)

      patternTuple <- FreeVarWriter[T].withNewFreeVarScope(
                        () =>
                          for {
                            patterns <- p.listname_.asScala.toList.traverse(n =>
                                          BoundVarWriter[T].withNewBoundVarScope(() => NormalizerRec[F].normalize(n)),
                                        )
                            reminder <- NormalizerRec[F].normalize(p.nameremainder_)
                            vars      = FreeVarReader[T].getFreeVars
                          } yield (ReceiveBindN(patterns, source, reminder, vars.size), vars),
                        insideReceive = true,
                      )

      (bind, freeVars) = patternTuple

      // Normalize body in the current bound and free variables scope
      continuation <- BoundVarWriter[T].withCopyBoundVarScope { () =>
                        BoundVarWriter[T].absorbFree(freeVars)
                        NormalizerRec[F].normalize(p.proc_)
                      }
    } yield ReceiveN(bind, continuation, persistent = true, peek = false, freeVars.size)
}
