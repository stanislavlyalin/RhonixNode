import org.scalatest.flatspec.AnyFlatSpec
import sdk.config.ConfigRender

class ReferenceConf extends AnyFlatSpec {
  "this" should "output reference configuration for simulation" in {
    val s =
      ConfigRender.referenceConf("gorki", sim.Config.Default, node.Config.Default, diagnostics.metrics.Config.Default)
    println(s)
  }
}
