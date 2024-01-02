package coop.rchain.rholang.normalizer2

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.interpreter.errors.TopLevelLogicalConnectivesNotAllowedError
import coop.rchain.rholang.normalizer2.util.Mock.*
import coop.rchain.rholang.normalizer2.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.ConnOrN
import io.rhonix.rholang.ast.rholang.Absyn.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class DisjunctionNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Disjunction normalizer"

  it should "convert AST term to ADT term" in {
    forAll { (s1: String, s2: String) =>
      val left  = new PGround(new GroundString(s1))
      val right = new PGround(new GroundString(s2))
      val term  = new PDisjunction(left, right)

      implicit val (mockRec, _, _, _, mockFVR) = createMockDSL[IO, VarSort](isTopLevel = false)

      val adt = DisjunctionNormalizer.normalizeDisjunction[IO, VarSort](term).unsafeRunSync()

      val expectedAdt = ConnOrN(Seq(mockADT(left: Proc), mockADT(right: Proc)))

      adt shouldBe expectedAdt

      val terms         = mockRec.extractData
      // Expect both sides of conjunction to be normalized in sequence
      val expectedTerms = Seq(TermData(ProcTerm(left)), TermData(ProcTerm(right)))

      terms shouldBe expectedTerms
    }
  }

  it should "throw an exception when attempting to normalize the top-level term" in {
    val term = new PDisjunction(new PNil, new PNil)

    // Create a mock DSL with the true `isTopLevel` flag (default value).
    implicit val (mockRec, _, _, _, mockFVR) = createMockDSL[IO, VarSort]()

    val thrown = intercept[TopLevelLogicalConnectivesNotAllowedError] {
      DisjunctionNormalizer.normalizeDisjunction[IO, VarSort](term).unsafeRunSync()
    }

    thrown.getMessage should include("disjunction")
  }
}
