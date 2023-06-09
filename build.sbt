import Dependencies.*

val scala3Version = "3.2.2"
val scala2Version = "2.13.10"

lazy val commonSettings = Seq(
  organization := "io.rhonix",
  version      := "0.1.0-SNAPSHOT",
//  scalafmtOnCompile := !sys.env.contains("CI"), // Format on compile, disable in CI environments
)

lazy val settingsScala3 = commonSettings ++ Seq(
  scalaVersion := scala3Version,
  scalacOptions ++= CompilerOptions.DEFAULTS,
  Compile / compile / wartremoverErrors ++= WartsSettings.DEFAULTS,
)

lazy val settingsScala2 = commonSettings ++ Seq(
  scalaVersion := scala2Version,
  // Enables Scala2 project to depend on Scala3 projects
  scalacOptions += "-Ytasty-reader",
  scalacOptions ++= CompilerOptions.DEFAULTS_SCALA_2,
  Compile / compile / wartremoverErrors ++= WartsSettings.DEFAULTS_SCALA_2,
)

lazy val sdk = (project in file("sdk"))
  .settings(settingsScala3*)
  .settings(
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests,
  )

// Consensus
lazy val weaver = (project in file("weaver"))
//  .settings(settingsScala3 *)
  .settings(settingsScala2*) // TEMP: Scala 2 project
  .settings(
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests,
  )
  .dependsOn(sdk)

// Node logic
lazy val dproc = (project in file("dproc"))
  .settings(settingsScala3*)
//  .settings(settingsScala2 *) // Compile error for Sync in classpath
  .settings(
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests,
  )
  .dependsOn(sdk, weaver)

// Node implementation
lazy val node = (project in file("node"))
  .settings(settingsScala3*)
//  .settings(settingsScala2 *) // Compilation of tests fail
  .settings(
    libraryDependencies ++= Seq(catsCore, catsEffect, protobuf, grpc, grpcNetty) ++ tests,
  )
  .dependsOn(sdk, weaver, dproc)
