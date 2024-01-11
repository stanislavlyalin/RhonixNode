package coop.rchain.rholang.normalizer2

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.VarSort
import coop.rchain.rholang.normalizer2.util.Mock.*
import coop.rchain.rholang.normalizer2.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.EMatchesN
import io.rhonix.rholang.ast.rholang.Absyn.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class MatchesNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Matches normalizer"

  it should "normalize AST term" in {
    forAll { (targetStr: String, patternStr: String) =>
      val targetTerm  = new PVar(new ProcVarVar(targetStr))
      val patternTerm = new PGround(new GroundString(patternStr))
      val matchesTerm = new PMatches(targetTerm, patternTerm)

      implicit val (nRec, bVScope, _, _, fVScope, _, _, _) = createMockDSL[IO, VarSort]()

      val adt = MatchesNormalizer.normalizeMatches[IO](matchesTerm).unsafeRunSync()

      val expectedAdt = EMatchesN(
        target = mockADT(targetTerm: Proc),
        pattern = mockADT(patternTerm: Proc),
      )

      adt shouldBe expectedAdt

      val terms = nRec.extractData

      val expectedTerms = Seq(
        TermData(ProcTerm(targetTerm)),
        TermData(term = ProcTerm(patternTerm), boundNewScopeLevel = 1, freeScopeLevel = 1),
      )

      terms shouldBe expectedTerms
    }
  }
}
