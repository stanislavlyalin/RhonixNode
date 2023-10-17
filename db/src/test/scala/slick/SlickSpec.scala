package slick

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
import sdk.data.Deploy
import sdk.db.RecordNotFound
import sdk.primitive.ByteArray
import slick.api.SlickApi
import slick.jdbc.JdbcProfile
import slick.syntax.all.*
import slick.tables.TableValidators.Validator

class SlickSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  implicit val validatorArbitrary: Arbitrary[Validator] = Arbitrary {
    for {
      id     <- Gen.posNum[Long]
      pubKey <- Gen.alphaStr.map(_.getBytes)
    } yield Validator(id, pubKey)
  }

  implicit val deployArbitrary: Arbitrary[Deploy] = Arbitrary {
    for {
      sig        <- Gen.alphaStr.map(_.getBytes)
      deployerPk <- Gen.alphaStr.map(_.getBytes)
      shardName  <- Gen.alphaStr
      program    <- Gen.alphaStr
      phloPrice  <- Gen.posNum[Long]
      phloLimit  <- Gen.posNum[Long]
      nonce      <- Gen.posNum[Long]
    } yield Deploy(ByteArray(sig), ByteArray(deployerPk), shardName, program, phloPrice, phloLimit, nonce)
  }

  "Validator insert function call" should "add the correct entry to the Validator table" in {
    forAll { (validator: Validator) =>
      def test(api: SlickApi[IO]) = for {
        id                <- api.validatorInsert(validator.pubKey)
        validatorById     <- OptionT(api.validatorGetById(id)).getOrRaise(new RecordNotFound)
        validatorByPubKey <- OptionT(api.validatorGetByPK(validator.pubKey)).getOrRaise(new RecordNotFound)
      } yield {
        id shouldBe 1L

        validatorById.pubKey shouldBe validator.pubKey
        validatorById.id shouldBe validatorByPubKey.id
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new SlickApi[IO])
        .use(test)
        .unsafeRunSync()
    }
  }

  "Deploy insert function call" should "add the correct entry to the Deploys, Deployers and Shards table" in {
    forAll { (d: Deploy) =>
      def test(api: SlickApi[IO]) = for {
        _            <- api.deployInsert(d)
        dFromDB      <- api.deployGet(d.sig)
        dList        <- api.deployGetAll
        deployerList <- api.deployerGetAll
        shardList    <- api.shardGetAll
      } yield {
        d shouldBe dFromDB.get
        dList shouldBe Seq(d.sig)
        deployerList shouldBe Seq(d.deployerPk)
        shardList shouldBe Seq(d.shardName)
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new SlickApi[IO])
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
