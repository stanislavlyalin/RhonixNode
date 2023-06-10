object CompilerOptions {

  /** Common scalac compiler options for Scala 3 */
  val DEFAULTS: Seq[String] = {
    // Scala 3 migration lookup table
    // https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html
    // format: off
    Seq(
      // Scala language features (standard settings)
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:experimental.macros",
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings", // Warning as compile errors
    )
  }
  // format: on

  /** Common scalac compiler options for Scala 2.13 */
  val DEFAULTS_SCALA_2: Seq[String] =
    // Scala 2 options
    // https://docs.scala-lang.org/overviews/compiler-options/index.html
    // format: off
    Seq(
      // Scala language features (standard settings)
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:experimental.macros",
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings", // Warning as compile errors
      "-Xsource:3",       // Use Scala 3 syntax in Scala 2 projects

      // Scala linter options
      "-Xlint:_",
      "-Xlint:-adapted-args",
      "-Wunused:-imports", // Disables warning for unused imports
      "-Wunused:-privates", // Disables warning for unused private vars
      "-Wunused:-locals", // Disables warning for unused local vars
      // Scala bug false positive warnings - https://github.com/scala/bug/issues/12072
      "-Xlint:-byname-implicit",

      // Warn when numerics are widened
      "-Wnumeric-widen",

      // To prevent "dead code following this construct" error when using `*` from mockito-scala library
      // https://github.com/mockito/mockito-scala/#dead-code-warning
      // This warning should be disabled only for Tests but in that case IntelliJ cannot compile project.
      // Related issues https://youtrack.jetbrains.com/issue/SCL-11824, https://youtrack.jetbrains.com/issue/IDEA-232043
      // "-Wdead-code",

      // TODO: Needed or duplicated with wartremover warnings?
      // Warn when non-Unit expression results are unused
      // "-Wvalue-discard",

      // With > 16: [error] invalid setting for -Ybackend-parallelism must be between 1 and 16
      // https://github.com/scala/scala/blob/v2.12.6/src/compiler/scala/tools/nsc/settings/ScalaSettings.scala#L240
      "-Ybackend-parallelism", java.lang.Runtime.getRuntime.availableProcessors().min(16).toString,
    )
    // format: on
}
