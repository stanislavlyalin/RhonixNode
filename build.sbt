import Dependencies._

lazy val projectSettings = Seq(organization := "io.rhonix", scalaVersion := "2.13.10", version := "0.1.0-SNAPSHOT")

lazy val commonSettings = projectSettings

// Consensus
lazy val weaver = (project in file("weaver"))
  .settings(commonSettings: _*)
  .settings(
    scalacOptions += "-Ytasty-reader", // resolves "error while loading package, Missing dependency 'Add -Ytasty-reader to scalac options to parse the TASTy in..."
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests
  )

// Node logic
lazy val dproc = (project in file("dproc"))
  .settings(commonSettings: _*)
  .settings(
    // The latest release leads to error, so 3.2.1 is used
    // [error] error while loading Discovery, class file '.../dproc/target/scala-3.3.0/classes/dproc/Discovery.class' is broken
    // [error] (class scala.tools.tasty.UnpickleException/TASTy signature has wrong version.
    // [error]  expected: {majorVersion: 28, minorVersion: 2}
    // [error]  found   : {majorVersion: 28, minorVersion: 3}
    // [error]
    // [error] This TASTy file was produced by a more recent, forwards incompatible release.
    // [error] To read this TASTy file, please upgrade your tooling.
    // [error] The TASTy file was produced by Scala 3.3.0.)
    scalaVersion := "3.2.1",
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests
  )
  .dependsOn(weaver)

// Node implementation
lazy val node = (project in file("node"))
  .settings(commonSettings: _*)
  .settings(
    scalacOptions += "-Ytasty-reader", // resolves "error while loading package, Missing dependency 'Add -Ytasty-reader to scalac options to parse the TASTy in..."
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, protobuf, grpc, grpcNetty) ++ tests
  )
  .dependsOn(dproc)
