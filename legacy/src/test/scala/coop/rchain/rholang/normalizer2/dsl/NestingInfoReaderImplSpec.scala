package coop.rchain.rholang.normalizer2.dsl

import coop.rchain.rholang.normalizer2.envimpl.NestingInfoReaderImpl
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class NestingInfoReaderImplSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  "NestingInfoReaderImpl" should "return correct values based on input functions" in {
    forAll { (insidePattern: Boolean, insideTopLevelReceivePattern: Boolean, insideBundle: Boolean) =>
      val reader = NestingInfoReaderImpl(() => insidePattern, () => insideTopLevelReceivePattern, () => insideBundle)
      reader.insidePattern shouldBe insidePattern
      reader.insideTopLevelReceivePattern shouldBe insideTopLevelReceivePattern
      reader.insideBundle shouldBe insideBundle
    }
  }
}
