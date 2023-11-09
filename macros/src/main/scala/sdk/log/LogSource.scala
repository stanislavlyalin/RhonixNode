package sdk.log

import scala.reflect.macros.blackbox

/** This is used to provide information about enclosing class for loggers. */
trait LogSource { val clazz: Class[?] }

object LogSource {
  def apply(c: Class[?]): LogSource = new LogSource { val clazz: Class[?] = c }

  // This implicit materializes an instance of log source using macro
  implicit def matLogSource: LogSource = macro LogSourceMacro.mkLogSource
}

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
