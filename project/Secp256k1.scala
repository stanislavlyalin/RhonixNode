import sbt.{file, pathToPathOps, taskKey, url}

import java.io.File
import scala.language.postfixOps
import scala.sys.process.*

object Secp256k1 {
  lazy val pullNative = taskKey[Seq[File]]("Pull native libs from remote repository")

  // TODO pull from https://github.com/gorki-network/secp256k1-native after CD if complete
  lazy val linux_x86_x64_remote =
    "https://github.com/nzpr/secp256k1-native/raw/aarch64/libs/secp256k1-native-linux-x86_64.so"
  lazy val osx_x86_x64_remote   =
    "https://github.com/nzpr/secp256k1-native/raw/aarch64/libs/secp256k1-native-osx-x86_64.dylib"
  lazy val osx_aarch64_remote   =
    "https://github.com/nzpr/secp256k1-native/raw/aarch64/libs/secp256k1-native-osx-aarch64.dylib"

  lazy val pullNativeLibs =
    taskKey[Unit]("Pull native libraries for Secp256k1")

  def pullSecp256k1(base: File): Set[File] = {
    println("Missing Secp256k1 native library, downloading...")
    new File(base.toPath.toString).mkdirs()
    val linux       = (base.toPath / "secp256k1-native-linux-x86_64.so").toFile
    val osx_x64     = (base.toPath / "secp256k1-native-osx-x86_64.dylib").toFile
    val osx_aarch64 = (base.toPath / "secp256k1-native-osx-aarch64.dylib").toFile

    url(linux_x86_x64_remote) #> file(linux.getAbsolutePath) !

    url(osx_x86_x64_remote) #> file(osx_x64.getAbsolutePath) !

    url(osx_aarch64_remote) #> file(osx_aarch64.getAbsolutePath) !

    Set(linux, osx_x64, osx_aarch64)
  }
}
