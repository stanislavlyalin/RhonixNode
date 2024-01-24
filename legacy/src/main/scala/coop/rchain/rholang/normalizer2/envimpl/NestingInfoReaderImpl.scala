package coop.rchain.rholang.normalizer2.envimpl

import coop.rchain.rholang.normalizer2.env.NestingInfoReader

final class NestingInfoReaderImpl(
  private val patternInfo: HistoryChain[(Boolean, Boolean)],
  private val bundleInfo: HistoryChain[Boolean],
) extends NestingInfoReader {

  override def insidePattern: Boolean = patternInfo.current()._1

  override def insideTopLevelReceivePattern: Boolean = patternInfo.current()._2

  override def insideBundle: Boolean = bundleInfo.current()
}

object NestingInfoReaderImpl {
  def apply(
    patternInfo: HistoryChain[(Boolean, Boolean)],
    bundleInfo: HistoryChain[Boolean],
  ): NestingInfoReaderImpl =
    new NestingInfoReaderImpl(patternInfo, bundleInfo)
}
