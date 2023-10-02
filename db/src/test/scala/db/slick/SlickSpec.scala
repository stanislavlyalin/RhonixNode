package db.slick

import cats.data.OptionT
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalacheck.ScalacheckShapeless.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdk.api.data.*
import sdk.db.*
import slick.api.*

class SlickSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  "Validator insert function call" should "add the correct entry to the Validator table" in {
    forAll { (validator: Validator) =>
      def test(api: ValidatorDbApiImplSlick[IO]) = for {
        id                <- api.insert(validator.publicKey)
        validatorById     <- OptionT(api.getById(id)).getOrRaise(new RecordNotFound)
        validatorByPubKey <- OptionT(api.getByPublicKey(validator.publicKey)).getOrRaise(new RecordNotFound)
      } yield {
        id shouldBe 1L

        validatorById.publicKey shouldBe validator.publicKey

        validatorById shouldBe validatorByPubKey
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new ValidatorDbApiImplSlick[IO])
        .use(test)
        .unsafeRunSync()
    }
  }
}
