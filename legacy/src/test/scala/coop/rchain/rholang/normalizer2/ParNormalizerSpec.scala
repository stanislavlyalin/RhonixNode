package coop.rchain.rholang.normalizer2

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.normalizer2.util.Mock.*
import io.rhonix.rholang.ast.rholang.Absyn.{GroundInt, GroundString, PGround, PPar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ParNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  "Par normalizer" should "normalize sequentially both terms" in {
    forAll { (s1: String, s2: String) =>
      val term1 = new PGround(new GroundString(s1))
      val term2 = new PGround(new GroundString(s2))
      // term1 | term2
      val par   = new PPar(term1, term2)

      implicit val (mockRec, _, _, _, _) = createMockDSL[IO, VarSort]()

      // Run Par normalizer
      ParNormalizer.normalizePar[IO](par).unsafeRunSync()

      val terms         = mockRec.extractData
      // Expect both sides of par to be normalized in sequence
      val expectedTerms = Seq(TermData(ProcTerm(term1)), TermData(ProcTerm(term2)))

      terms shouldBe expectedTerms
    }
  }
}
