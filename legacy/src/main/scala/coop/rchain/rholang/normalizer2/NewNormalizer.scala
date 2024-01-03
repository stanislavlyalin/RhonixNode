package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import cats.syntax.all.*
import coop.rchain.rholang.interpreter.compiler.normalizer.GroundNormalizeMatcher
import coop.rchain.rholang.interpreter.compiler.{NameSort, SourcePosition, VarSort}
import coop.rchain.rholang.normalizer2.env.{BoundVarReader, BoundVarWriter}
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*

import scala.jdk.CollectionConverters.*

object NewNormalizer {
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def normalizeNew[F[_]: Sync: NormalizerRec, T >: VarSort: BoundVarWriter: BoundVarReader](p: PNew): F[NewN] =
    Sync[F].defer {
      // TODO: bindings within a single new shouldn't have overlapping names.
      val newTaggedBindings = p.listnamedecl_.asScala.toVector.map {
        case n: NameDeclSimpl => (None, n.var_, NameSort, n.line_num, n.col_num)
        case n: NameDeclUrn   =>
          (
            Some(GroundNormalizeMatcher.stripUri(n.uriliteral_)),
            n.var_,
            NameSort,
            n.line_num,
            n.col_num,
          )
      }
      // This sorts the None's first, and the uris by lexicographical order.
      // We do this here because the sorting affects the numbering of variables inside the body.
      val sortBindings      = newTaggedBindings.sortBy(row => row._1)
      val newBindings       = sortBindings.map(row => (row._2, row._3, SourcePosition(row._4, row._5)))
      val uris              = sortBindings.flatMap(row => row._1)

      val initCount = BoundVarReader[T].boundVarCount
      BoundVarWriter[T].putBoundVars(newBindings.toList)
      val bindCount = BoundVarReader[T].boundVarCount - initCount

      NormalizerRec[F]
        .normalize(p.proc_)
        .map(par => NewN(bindCount = bindCount, p = par, uri = uris, injections = Map[String, ParN]()))
    }
}
