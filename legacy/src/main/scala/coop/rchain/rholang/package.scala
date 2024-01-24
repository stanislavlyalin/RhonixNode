package coop.rchain

import coop.rchain.metrics.Metrics
import coop.rchain.rholang.interpreter.*
import coop.rchain.rholang.normalizer2.syntax.*

package object rholang {
  val RholangMetricsSource: Metrics.Source = Metrics.Source(Metrics.BaseSource, "rholang")

  object syntax extends AllSyntaxRholang
}

trait AllSyntaxRholang
    extends RhoRuntimeSyntax
    with RhoHistoryRepositorySyntax
    with NormalizerSyntax
    with HistoryChainSyntax
