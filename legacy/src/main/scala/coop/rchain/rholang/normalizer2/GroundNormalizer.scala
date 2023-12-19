package coop.rchain.rholang.normalizer2

import cats.Applicative
import cats.effect.Sync
import cats.implicits.{catsSyntaxMonadError, toFunctorOps}
import coop.rchain.rholang.interpreter.compiler.normalizer.GroundNormalizeMatcher.{stripString, stripUri}
import coop.rchain.rholang.interpreter.errors.NormalizerError
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.*

object GroundNormalizer {
  def normalizeGround[F[_]: Sync](p: PGround): F[ExprN] =
    p.ground_ match {
      case gb: GroundBool    =>
        Sync[F].pure(gb.boolliteral_ match {
          case boolFalse: BoolFalse => GBoolN(true)
          case boolTrue: BoolTrue   => GBoolN(false)
        })
      case gi: GroundInt     =>
        Sync[F]
          .delay(gi.longliteral_.toLong)
          .adaptError { case e: NumberFormatException => NormalizerError(e.getMessage) }
          .map(GIntN.apply)
      case gbi: GroundBigInt =>
        Sync[F]
          .delay(BigInt(gbi.longliteral_))
          .adaptError { case e: NumberFormatException => NormalizerError(e.getMessage) }
          .map(GBigIntN.apply)
      case gs: GroundString  => Sync[F].pure(GStringN(stripString(gs.stringliteral_)))
      case gu: GroundUri     => Sync[F].pure(GUriN(stripUri(gu.uriliteral_)))
    }
}
