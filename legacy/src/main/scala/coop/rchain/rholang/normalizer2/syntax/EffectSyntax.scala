package coop.rchain.rholang.normalizer2.syntax

import cats.effect.Sync
import cats.implicits.{toFlatMapOps, toFunctorOps}
import coop.rchain.rholang.interpreter.compiler.FreeContext
import coop.rchain.rholang.normalizer2.env.{BoundVarScope, BoundVarWriter, FreeVarScope}

trait EffectSyntax {
  implicit def normalizerEffectSyntax[F[_], A](f: F[A]): NormalizerEffectOps[F, A] = new NormalizerEffectOps[F, A](f)
}

class NormalizerEffectOps[F[_], A](val f: F[A]) extends AnyVal {

  /**
   * Run effect inside a new (empty) bound and free variable context/scope.
   *
   * Empty variable context is used to normalize patterns.
   */
  def withNewVarScope(
    insideReceive: Boolean = false,
  )(implicit fwScope: FreeVarScope[F], bwScope: BoundVarScope[F]): F[A] =
    bwScope.withNewBoundVarScope(fwScope.withNewFreeVarScope(insideReceive)(f))

  /** Bound free variables in a copy of the current scope.
   *
   * Free variables are sorted by levels and then added with indexes:
   * {i0, i1, ..., iN} = {fl0 + last + 1, fl1 + last + 1, ..., flN + last + 1}.
   * Here, i0, ..., iN represent the Bruijn indices of the new bound vars,
   * fl0, ..., flN are the Bruijn levels of the inserted free vars,
   * last is the last index among all bound vars at the moment.
   */

  def withAbsorbedFreeVars[T](
    freeVars: Seq[(String, FreeContext[T])],
  )(implicit sync: Sync[F], bwScope: BoundVarScope[F], bwWriter: BoundVarWriter[T]): F[A] = {

    @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
    def absorbFree(freeVars: Seq[(String, FreeContext[T])]): Unit = {
      val sortedByLevel  = freeVars.sortBy(_._2.level)
      val (levels, data) = sortedByLevel.unzip(fv => (fv._2.level, (fv._1, fv._2.typ, fv._2.sourcePosition)))
      assert(
        levels == levels.indices,
        "Error when absorbing free variables during normalization: incorrect de Bruijn levels." +
          s"Should be ${levels.indices}, but was $levels.",
      )
      bwWriter.putBoundVars(data)
    }

    bwScope.withCopyBoundVarScope(sync.delay(absorbFree(freeVars)).flatMap(_ => f))
  }
}
