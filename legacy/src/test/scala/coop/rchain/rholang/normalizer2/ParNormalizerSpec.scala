package coop.rchain.rholang.normalizer2

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import io.rhonix.rholang.ast.rholang.Absyn.{GroundInt, Name, PGround, PPar, Proc}
import io.rhonix.rholang.{NilN, ParN}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ParNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  "Par normalizer" should "delegate recursive call to the dependency" in {
    forAll { (s1: String, s2: String) =>
      // Construct mock Par object
      val i1  = new GroundInt(s1)
      val i2  = new GroundInt(s2)
      val g1  = new PGround(i1)
      val g2  = new PGround(i2)
      val par = new PPar(g1, g2)

      import collection.mutable.ListBuffer

      // List of actual arguments to recursive call
      val args = ListBuffer[Proc]()

      // Mock implementation of recursive normalizer
      implicit val normalizerRec = new NormalizerRec[IO] {
        override def normalize(proc: Proc): IO[ParN] = {
          args.append(proc)
          NilN.pure[IO]
        }

        // TODO: Check calls to normalize Name also.
        override def normalize(proc: Name): IO[ParN] = ???
      }

      // Run Par normalizer
      ParNormalizer.normalizePar[IO](par).unsafeRunSync()

      // Expect both sides of par to be normalized in sequence
      args shouldBe ListBuffer(g1, g2)
    }
  }

}
