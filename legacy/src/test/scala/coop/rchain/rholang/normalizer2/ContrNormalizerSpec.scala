package coop.rchain.rholang.normalizer2

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.interpreter.compiler.{NameSort, VarSort}
import coop.rchain.rholang.normalizer2.util.Mock.*
import coop.rchain.rholang.normalizer2.util.MockNormalizerRec.{mockADT, RemainderADTDefault}
import io.rhonix.rholang.ast.rholang.Absyn.*
import io.rhonix.rholang.{ReceiveBindN, ReceiveN}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ContrNormalizerSpec extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  behavior of "Contr normalizer"

  it should "convert AST term to ADT term" in {
    forAll(Arbitrary.arbitrary[String], Gen.nonEmptyListOf[String](Arbitrary.arbitrary[String])) {
      (sourceStr: String, strings: Seq[String]) =>
        val source       = new NameVar(sourceStr)
        val patterns     = strings.distinct.map(new NameVar(_))
        val remainder    = new NameRemainderEmpty()
        val continuation = new PNil

        val listPatterns = new ListName()
        patterns.foreach(listPatterns.add)

        // contract source (pattern1, pattern2, ... remainder) { continuation }
        val term = new PContr(source, listPatterns, remainder, continuation)

        implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, _) = createMockDSL[IO, VarSort]()

        val adt = ContrNormalizer.normalizeContr[IO, VarSort](term).unsafeRunSync()

        val expectedAdt = ReceiveN(
          bind = ReceiveBindN(
            patterns = patterns.map(mockADT),
            source = mockADT(source),
            remainder = RemainderADTDefault,
            freeCount = 0, // We don't care about free variables in this test
          ),
          body = mockADT(continuation),
          persistent = true,
          peek = false,
          bindCount = 0, // We don't care about free variables in this test
        )

        adt shouldBe expectedAdt

        val terms         = nRec.extractData
        // Expect all terms to be normalized in sequence
        val expectedTerms = TermData(NameTerm(source)) +:
          patterns.map(x => TermData(NameTerm(x), boundNewScopeLevel = 1, freeScopeLevel = 1)) :+
          TermData(NameRemainderTerm(remainder), boundNewScopeLevel = 1, freeScopeLevel = 1) :+
          TermData(ProcTerm(continuation), boundCopyScopeLevel = 1)
        terms shouldBe expectedTerms
    }
  }

  it should "bind free variables before case body normalization" in {
    val listPatterns = new ListName()
    listPatterns.add(new NameVar(""))
    val term         = new PContr(new NameVar(""), listPatterns, new NameRemainderEmpty(), new PNil)

    implicit val (nRec, bVScope, bVW, _, fVScope, _, fVR, _) = createMockDSL[IO, VarSort](
      initFreeVars = Seq(VarReaderData("x", 0, NameSort), VarReaderData("y", 1, NameSort)),
    )

    ContrNormalizer.normalizeContr[IO, VarSort](term).unsafeRunSync()

    val addedBoundVars = bVW.extractData

    // Absorbed free variables and bind them in a copy of the scope
    val expectedBoundVars = Seq(
      BoundVarWriterData(name = "x", varType = NameSort, copyScopeLevel = 1),
      BoundVarWriterData(name = "y", varType = NameSort, copyScopeLevel = 1),
    )
    addedBoundVars shouldBe expectedBoundVars
  }

}
