import Dependencies._

lazy val projectSettings = Seq(organization := "io.rhonix", scalaVersion := "2.13.10", version := "0.1.0-SNAPSHOT")

lazy val commonSettings = projectSettings

// Consensus
lazy val weaver = (project in file("weaver"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests
  )

// Node logic
lazy val dproc = (project in file("dproc"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, fs2Core) ++ tests
  )
  .dependsOn(weaver)

// Node implementation
lazy val node = (project in file("node"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.1",
    libraryDependencies ++= Seq(catsCore, catsEffect, protobuf, grpc, grpcNetty) ++ tests
  )
  .dependsOn(dproc)
