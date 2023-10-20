package slick

import cats.data.OptionT
import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO}
import cats.syntax.all.*
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
import org.scalacheck.ScalacheckShapeless.derivedArbitrary

class SlickSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  // Define Arbitrary for ByteArray since it's a custom type and needs specific generation logic
  implicit val byteArrayArbitrary: Arbitrary[ByteArray] = Arbitrary {
    Gen.nonEmptyListOf(Gen.alphaChar).map(chars => ByteArray(chars.mkString.getBytes))
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

  "deployInsert() function call" should "add the correct entry to the Deploys, Deployers and Shards table" in {
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

  "deployInsert() function call" should "not duplicate records in the Deployers and Shards tables if they are the same" in {
    forAll { (d1: Deploy, d2Sig: ByteArray) =>
      val d2: Deploy              = d1.copy(sig = d2Sig) // create d2 with the same fields but another sig
      def test(api: SlickApi[IO]) = for {
        _            <- api.deployInsert(d1)
        _            <- api.deployInsert(d2)
        d1FromDB     <- api.deployGet(d1.sig)
        d2FromDB     <- api.deployGet(d2.sig)
        dList        <- api.deployGetAll
        deployerList <- api.deployerGetAll
        shardList    <- api.shardGetAll
      } yield {
        d1 shouldBe d1FromDB.get
        d2 shouldBe d2FromDB.get
        dList.toSet shouldBe Seq(d1.sig, d2.sig).toSet
        deployerList shouldBe Seq(d1.deployerPk)
        shardList shouldBe Seq(d1.shardName)
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new SlickApi[IO])
        .use(test)
        .unsafeRunSync()
    }
  }

  "deployDelete() function call" should "remove deploy and clean up dependencies in Deployers and Shards tables if possible" in {
    forAll { (d1: Deploy, d2Sig: ByteArray) =>
      val d2: Deploy = d1.copy(sig = d2Sig) // create d2 with the same fields but another sig

      def test(api: SlickApi[IO]) = for {
        // Creating two deploys with the same data but different sig
        _ <- api.deployInsert(d1)
        _ <- api.deployInsert(d2)

        // First delete action (removing d1 and read db data)
        _                 <- api.deployDelete(d1.sig)
        dListFirst        <- api.deployGetAll
        deployerListFirst <- api.deployerGetAll
        shardListFirst    <- api.shardGetAll

        // Second delete action (removing d2 and read db data)
        _                  <- api.deployDelete(d2.sig)
        dListSecond        <- api.deployGetAll
        deployerListSecond <- api.deployerGetAll
        shardListSecond    <- api.shardGetAll
      } yield {
        dListFirst shouldBe Seq(d2.sig)
        // The first action should not clear the tables Deployers and Shards. Because it using in d2
        deployerListFirst shouldBe Seq(d1.deployerPk)
        shardListFirst shouldBe Seq(d1.shardName)

        dListSecond shouldBe Seq()
        // The second action should clear the tables Deployers and Shards. Because deploys deleted
        deployerListSecond shouldBe Seq()
        shardListSecond shouldBe Seq()
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
