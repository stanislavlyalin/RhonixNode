package coop.rchain.rholang.normalizer2.util

import cats.effect.Sync
import cats.implicits.*
import coop.rchain.rholang.normalizer2.env.{FreeVarScope, RestrictWriter}

case class MockRestrictWriter[F[_]: Sync]() extends RestrictWriter[F] {
  private var insidePatternFlag: Boolean                = false
  private var insideTopLevelReceivePatternFlag: Boolean = false
  private var insideBundleFlag: Boolean                 = false

  override def restrictAsPattern[R](inReceive: Boolean)(scopeFn: F[R]): F[R] = for {
    insidePatternFlagRestore               <- Sync[F].delay(insidePatternFlag)
    _                                       = insidePatternFlag = true
    insideTopLevelReceivePatternFlagRestore = insideTopLevelReceivePatternFlag
    _                                       = insideTopLevelReceivePatternFlag = inReceive
    res                                    <- scopeFn
    _                                       = insidePatternFlag = insidePatternFlagRestore
    _                                       = insideTopLevelReceivePatternFlag = insideTopLevelReceivePatternFlagRestore
  } yield res

  override def restrictAsBundle[R](scopeFn: F[R]): F[R] = for {
    insideBundleFlagRestore <- Sync[F].delay(insideBundleFlag)
    _                        = insideBundleFlag = true
    res                     <- scopeFn
    _                        = insideBundleFlag = insideBundleFlagRestore
  } yield res

  def getInsidePatternFlag: Boolean                = insidePatternFlag
  def getInsideTopLevelReceivePatternFlag: Boolean = insideTopLevelReceivePatternFlag
  def getInsideBundleFlag: Boolean                 = insideBundleFlag
}
