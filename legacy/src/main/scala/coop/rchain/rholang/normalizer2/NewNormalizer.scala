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
  def normalizeNew[F[_]: Sync: NormalizerRec, T >: VarSort: BoundVarWriter](p: PNew): F[NewN] =
    Sync[F].defer {
      val simpleBindings = p.listnamedecl_.asScala.toSeq.collect { case n: NameDeclSimpl =>
        (n.var_, NameSort, SourcePosition(n.line_num, n.col_num))
      } // Unsorted simple bindings

      val sortedUrnData = p.listnamedecl_.asScala.toSeq
        .collect { case n: NameDeclUrn =>
          (
            GroundNormalizeMatcher.stripUri(n.uriliteral_),
            (n.var_, NameSort, SourcePosition(n.line_num, n.col_num)),
          )
        }
        .sortBy(_._1) // Sort by uris in lexicographical order

      val (uris, urnBindings) = sortedUrnData.unzip

      val bindCount = BoundVarWriter[T].putBoundVars(simpleBindings ++ urnBindings)

      NormalizerRec[F]
        .normalize(p.proc_)
        .map(par => NewN(bindCount = bindCount, p = par, uri = uris, injections = Map[String, ParN]()))
    }
}
