import sbt._

object Dependencies {
  val catsCore = ("org.typelevel" %% "cats-core" % "2.9.0").cross(CrossVersion.for2_13Use3)
  val catsEffect = ("org.typelevel" %% "cats-effect" % "3.4.8").cross(CrossVersion.for2_13Use3)
  val fs2Core = ("co.fs2" %% "fs2-core" % "3.6.1").cross(CrossVersion.for2_13Use3)

  val protobuf = "com.google.protobuf" % "protobuf-java" % "3.22.2"
  val grpc = "io.grpc" % "grpc-core" % "1.53.0"
  val grpcNetty = "io.grpc" % "grpc-netty" % "1.53.0"

  val scalatest = ("org.scalatest" %% "scalatest" % "3.2.15" % Test).cross(CrossVersion.for2_13Use3)
  val scalatest_ce =
    ("org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test).cross(CrossVersion.for2_13Use3)

  val tests = Seq(scalatest, scalatest_ce)
}
