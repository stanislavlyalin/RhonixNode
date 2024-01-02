package coop.rchain.rholang.normalizer2

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{ProcSort, VarSort}
import coop.rchain.rholang.normalizer2.util.Mock.*
import coop.rchain.rholang.normalizer2.util.MockNormalizerRec.mockADT
import io.rhonix.rholang.{MatchCaseN, MatchN}
import io.rhonix.rholang.ast.rholang.Absyn.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class MatchNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Match normalizer"

  it should "convert AST term to match ADT term" in {
    forAll { (targetStr: String, pattern1Str: String, pattern2Str: String, b: Boolean) =>
      val targetTerm    = new PVar(new ProcVarVar(targetStr))
      val pattern1Term  = new PGround(new GroundString(pattern1Str))
      val caseBody1Term = new PNil
      val pattern2Term  = new PVar(new ProcVarVar(pattern2Str))
      val caseBody2Term = new PGround(new GroundBool(if (b) new BoolTrue else new BoolFalse))

      // match target { case pattern1 => caseBody1; case pattern2 => caseBody2 }
      val cases     = List(
        new CaseImpl(pattern1Term, caseBody1Term),
        new CaseImpl(pattern2Term, caseBody2Term),
      )
      val listCases = new ListCase()
      cases.foreach(listCases.add)
      val inputTerm = new PMatch(targetTerm, listCases)

      implicit val (mockRec, mockBVW, _, mockFVW, mockFVR) = createMockDSL[IO, VarSort]()

      val adt = MatchNormalizer.normalizeMatch[IO, VarSort](inputTerm).unsafeRunSync()

      val expectedAdt = MatchN(
        target = mockADT(targetTerm: Proc),
        cases = Seq(
          MatchCaseN(pattern = mockADT(pattern1Term: Proc), source = mockADT(caseBody1Term: Proc)),
          MatchCaseN(pattern = mockADT(pattern2Term: Proc), source = mockADT(caseBody2Term: Proc)),
        ),
      )

      adt shouldBe expectedAdt

      val terms = mockRec.extractData

      // Expect all terms to be normalized in sequence
      val expectedTerms = Seq(
        TermData(ProcTerm(targetTerm)),
        TermData(term = ProcTerm(pattern1Term), boundNewScopeLevel = 1, freeScopeLevel = 1),
        TermData(term = ProcTerm(caseBody1Term), boundCopyScopeLevel = 1),
        TermData(term = ProcTerm(pattern2Term), boundNewScopeLevel = 1, freeScopeLevel = 1),
        TermData(term = ProcTerm(caseBody2Term), boundCopyScopeLevel = 1),
      )

      terms shouldBe expectedTerms
    }
  }

  it should "bind free variables before case body normalization" in {
    val cases     = List(new CaseImpl(new PNil, new PNil))
    val listCases = new ListCase()
    cases.foreach(listCases.add)
    // match Nil { case Nil => Nil}
    val term      = new PMatch(new PNil, listCases)

    implicit val (mockRec, mockBVW, _, mockFVW, mockFVR) = createMockDSL[IO, VarSort](
      initFreeVars = Seq(VarReaderData("x", 0, ProcSort), VarReaderData("y", 1, ProcSort)),
    )

    MatchNormalizer.normalizeMatch[IO, VarSort](term).unsafeRunSync()

    val addedBoundVars = mockBVW.extractData

    // Absorbed free variables and bind them in a copy of the scope
    val expectedBoundVars = Seq(
      BoundVarWriterData(name = "x", varType = ProcSort, copyScopeLevel = 1),
      BoundVarWriterData(name = "y", varType = ProcSort, copyScopeLevel = 1),
    )

    addedBoundVars shouldBe expectedBoundVars
  }
}
