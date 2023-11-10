package sdk.log

import scala.reflect.macros.blackbox

/** Macro implementation reads enclosing class using reflection API and creates LogSource instance. */
class LogSourceMacro(val c: blackbox.Context) {
  import c.universe.*

  def mkLogSource: c.Expr[LogSource] = {
    val tree =
      q"""
         sdk.log.LogSource(${c.reifyEnclosingRuntimeClass}.asInstanceOf[Class[?]])
       """

    c.Expr[LogSource](tree)
  }
}

object LogSourceMacroInstance {
  // This implicit materializes an instance of log source using macro
  implicit def logSource: LogSource = macro LogSourceMacro.mkLogSource
}
