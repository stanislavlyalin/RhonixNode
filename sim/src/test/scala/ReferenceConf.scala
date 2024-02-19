import org.scalatest.flatspec.AnyFlatSpec
import sdk.reflect.ClassesAsConfig

class ReferenceConf extends AnyFlatSpec {
  "this" should "output reference configuration for simulation" in {
    val s = ClassesAsConfig(
      "gorki",
      sim.Config.Default,
      node.Config.Default,
      diagnostics.metrics.Config.Default,
      db.Config.Default,
      node.comm.Config.Default,
    )
    println(s)
  }

  "this" should "produce configuration parameter names" in {
    val paramNames = ClassesAsConfig
      .kvMap(
        "gorki",
        sim.Config.Default,
        node.Config.Default,
        diagnostics.metrics.Config.Default,
        db.Config.Default,
        node.comm.Config.Default,
      )
      .keys
      .mkString("\n")
    println(paramNames)
  }
}
