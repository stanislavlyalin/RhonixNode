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
import slick.SlickDb
import slick.api.*

// TODO re-enable these tests when it is possible to execute them on CI.
// At the moment Github Actions does not allow non-root users and embedded postgres requires non-root.
// https://github.com/fergusstrange/embedded-postgres/issues/95
class SlickSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  "Validator insert function call" should "add the correct entry to the Validator table" ignore {
    forAll { (validator: Validator) =>
      def test(api: ValidatorDbApiImplSlick[IO]) = for {
        id                <- api.insert(validator)
        validatorById     <- OptionT(api.getById(id)).getOrRaise(new RecordNotFound)
        validatorByPubKey <- OptionT(api.getByPublicKey(validator.publicKey)).getOrRaise(new RecordNotFound)
      } yield {
        id shouldBe 1L

        validatorById.publicKey shouldBe validator.publicKey

        validatorById shouldBe validatorByPubKey
      }

      EmbeddedPgSlickDb[IO]
        .map { case SlickDb(db, profile, _) => new ValidatorDbApiImplSlick[IO](db, profile) }
        .use(test)
        .unsafeRunSync()
    }
  }
}
