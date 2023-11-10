package sdk.log

/** This is used to provide information about enclosing class for loggers. */
trait LogSource { val clazz: Class[?] }

object LogSource {
  def apply(c: Class[?]): LogSource = new LogSource { val clazz: Class[?] = c }
}
