import Dependencies._

val scalaVersionDefault = "3.2.1"
val scala2Version = "2.13.10"

lazy val projectSettings = Seq(
  organization := "io.rhonix",
  scalaVersion := scala2Version,
  version := "0.1.0-SNAPSHOT",
  Compile / compile / wartremoverErrors ++= Warts.allBut(
    // those we want
    Wart.DefaultArguments,
    Wart.ImplicitParameter,
    Wart.ImplicitConversion,
    Wart.LeakingSealed,
    Wart.Recursion,
    // those don't want
    Wart.Overloading,
    Wart.Nothing,
    Wart.Equals,
    Wart.PublicInference,
    Wart.ArrayEquals,
    Wart.While,
    Wart.Any,
    Wart.Product,
    Wart.Serializable,
    Wart.OptionPartial,
    Wart.Option2Iterable,
    Wart.ToString,
    Wart.MutableDataStructures,
    Wart.FinalVal,
    Wart.Null,
    Wart.AsInstanceOf,
    Wart.ExplicitImplicitTypes,
    Wart.StringPlusAny,
    Wart.AnyVal,
    // Added after migration to Scala 2.13
    Wart.TripleQuestionMark,
    Wart.IterableOps,
    Wart.JavaSerializable,
    Wart.ListUnapply,
    Wart.GlobalExecutionContext,
    Wart.NoNeedImport,
    Wart.PlatformDefault,
    Wart.JavaNetURLConstructors,
    Wart.SizeIs,
    Wart.SizeToLength,
    Wart.ListAppend,
    Wart.AutoUnboxing,
    Wart.RedundantConversions
  )
)

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
    scalaVersion := scalaVersionDefault,
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests
  )
  .dependsOn(weaver)

lazy val sdk = (project in file("sdk"))
  .settings(commonSettings: _*)
  .settings(
    scalaVersion := scalaVersionDefault,
    version := "0.1"
  )
  .dependsOn(weaver, dproc)

// Node implementation
lazy val node = (project in file("node"))
  .settings(commonSettings: _*)
  .settings(
    scalacOptions += "-Ytasty-reader", // resolves "error while loading package, Missing dependency 'Add -Ytasty-reader to scalac options to parse the TASTy in..."
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, protobuf, grpc, grpcNetty) ++ tests
  )
  .dependsOn(dproc, sdk)
