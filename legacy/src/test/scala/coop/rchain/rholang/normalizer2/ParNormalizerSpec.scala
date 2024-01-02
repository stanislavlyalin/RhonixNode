package coop.rchain.rholang.normalizer2

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.normalizer2.util.Mock.*
import io.rhonix.rholang.ast.rholang.Absyn.{GroundString, PGround, PPar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ParNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  "Par normalizer" should "normalize sequentially both terms" in {
    forAll { (s1: String, s2: String) =>
      val left  = new PGround(new GroundString(s1))
      val right = new PGround(new GroundString(s2))
      // term1 | term2
      val term  = new PPar(left, right)

      implicit val (mockRec, _, _, _, _) = createMockDSL[IO, VarSort]()

      // Run Par normalizer
      ParNormalizer.normalizePar[IO](term).unsafeRunSync()

      val terms         = mockRec.extractData
      // Expect both sides of par to be normalized in sequence
      val expectedTerms = Seq(TermData(ProcTerm(left)), TermData(ProcTerm(right)))

      terms shouldBe expectedTerms
    }
  }
}
