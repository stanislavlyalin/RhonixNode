package db.slick

import cats.data.OptionT
import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO}
import cats.syntax.all.*
import org.scalacheck.ScalacheckShapeless.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.Assertion
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import slick.jdbc.JdbcProfile
import slick.syntax.all.*
import slick.{SlickDb, SlickQuery}
import sdk.db.RecordNotFound
import slick.api.ValidatorDbApiImplSlick
import slick.data.Validator

class SlickSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  implicit val validatorArbitrary: Arbitrary[Validator] = Arbitrary {
    for {
      id     <- Gen.posNum[Long]
      pubKey <- Gen.alphaStr.map(_.getBytes)
    } yield Validator(id, pubKey)
  }

  "Validator insert function call" should "add the correct entry to the Validator table" in {
    forAll { (validator: Validator) =>
      def test(api: ValidatorDbApiImplSlick[IO]) = for {
        id                <- api.insert(validator.pubKey)
        validatorById     <- OptionT(api.getById(id)).getOrRaise(new RecordNotFound)
        validatorByPubKey <- OptionT(api.getByPublicKey(validator.pubKey)).getOrRaise(new RecordNotFound)
      } yield {
        id shouldBe 1L

        validatorById.pubKey shouldBe validator.pubKey
        validatorById.id shouldBe validatorByPubKey.id
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new ValidatorDbApiImplSlick[IO])
        .use(test)
        .unsafeRunSync()
    }
  }

  "Stored and loaded name-value pairs" should "be the same" in {
    forAll { (name: String, value: String) =>
      def test[F[_]: Async](storeF: => F[Int], loadF: => F[Option[String]]): F[Assertion] = for {
        _         <- storeF
        extracted <- OptionT(loadF).getOrRaise(new RuntimeException("Failed to get value by name"))
      } yield extracted shouldBe value

      EmbeddedH2SlickDb[IO]
        .use { implicit slickDb =>
          implicit val profile: JdbcProfile = SlickDb[IO].profile
          implicit val async                = Async[IO]
          val queries: SlickQuery           = SlickQuery()
          import queries.*
          test[IO](storeValue(name, value).run, loadValue(name).run)
        }
        .unsafeRunSync()
    }
  }
}
