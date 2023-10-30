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
import sdk.data.{Block, Deploy}
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
        dList shouldBe Set(d.sig)
        deployerList shouldBe Set(d.deployerPk)
        shardList shouldBe Set(d.shardName)
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
        dList shouldBe Set(d1.sig, d2.sig)
        deployerList shouldBe Set(d1.deployerPk)
        shardList shouldBe Set(d1.shardName)
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
        dListFirst shouldBe Set(d2.sig)
        // The first action should not clear the tables Deployers and Shards. Because it using in d2
        deployerListFirst shouldBe Set(d1.deployerPk)
        shardListFirst shouldBe Set(d1.shardName)

        dListSecond shouldBe Set()
        // The second action should clear the tables Deployers and Shards. Because deploys deleted
        deployerListSecond shouldBe Set()
        shardListSecond shouldBe Set()
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new SlickApi[IO])
        .use(test)
        .unsafeRunSync()
    }
  }

  val nonEmptyDeploySeqGen: Gen[Set[Deploy]] = for {
    size    <- Gen.chooseNum(1, 10) // Choose a suitable max value
    deploys <- Gen.listOfN(size, Arbitrary.arbitrary[Deploy])
  } yield deploys.toSet

  "deploySetInsert() function call" should "add the correct entry to the DeploySets and DeploySetBinds tables" in {
    forAll(nonEmptyDeploySeqGen, Arbitrary.arbitrary[ByteArray]) { (deploys: Set[Deploy], dSetHash: ByteArray) =>
      def test(api: SlickApi[IO]) = for {
        _         <- deploys.toSeq.traverse(api.deployInsert)
        deploySigs = deploys.map(_.sig)
        _         <- api.deploySetInsert(dSetHash, deploySigs)

        dSet     <- api.deploySetGet(dSetHash)
        dSetList <- api.deploySetGetAll
      } yield {
        deploySigs shouldBe dSet.get
        dSetList shouldBe Set(dSetHash)
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new SlickApi[IO])
        .use(test)
        .unsafeRunSync()
    }
  }
  val nonEmptyBondsMapGen: Gen[Map[ByteArray, Long]] = for {
    size  <- Gen.chooseNum(1, 10) // Choose a suitable max value
    bonds <- Gen.listOfN(size, Arbitrary.arbitrary[(ByteArray, Long)])
  } yield bonds.toMap

  "bondsMapInsert() function call" should "add the correct entry to the BondsMaps and Bonds tables" in {
    forAll(Arbitrary.arbitrary[ByteArray], nonEmptyBondsMapGen) { (bMapHash: ByteArray, bMap: Map[ByteArray, Long]) =>
      def test(api: SlickApi[IO]) = for {
        _ <- api.bondsMapInsert(bMapHash, bMap)

        readBMap <- api.bondsMapGet(bMapHash)
        bMapList <- api.bondsMapGetAll
      } yield {
        bMap shouldBe readBMap.get
        bMapList.toSet shouldBe Seq(bMapHash).toSet
      }

      EmbeddedH2SlickDb[IO]
        .map(implicit x => new SlickApi[IO])
        .use(test)
        .unsafeRunSync()
    }
  }

  "blockInsert() function call" should "add the correct entry to the Blocks table and to the all related tables" in {
    forAll(
      Arbitrary.arbitrary[ByteArray],
      nonEmptyDeploySeqGen,
      Arbitrary.arbitrary[ByteArray],
      nonEmptyBondsMapGen,
      Arbitrary.arbitrary[Block],
    ) { (dSetHash: ByteArray, deploys: Set[Deploy], bMapHash: ByteArray, bMap: Map[ByteArray, Long], b) =>
      def test(api: SlickApi[IO]) = for {
        _            <- deploys.toSeq.traverse(api.deployInsert)
        deploySigs    = deploys.map(_.sig)
        insertedBlock = sdk.data.Block(
                          version = b.version,
                          hash = b.hash,
                          sigAlg = b.sigAlg,
                          signature = b.signature,
                          finalStateHash = b.finalStateHash,
                          postStateHash = b.postStateHash,
                          validatorPk = b.validatorPk,
                          shardName = b.shardName,
                          justificationSet = Set(),
                          seqNum = b.seqNum,
                          offencesSet = Set(),
                          bondsMap = bMap,
                          finalFringe = Set(),
                          deploySet = deploySigs,
                          mergeSet = Set(),
                          dropSet = Set(),
                          mergeSetFinal = Set(),
                          dropSetFinal = Set(),
                        )
        _            <- api.blockInsert(insertedBlock)(None, None, bMapHash, None, Some(dSetHash), None, None, None, None)

        readBlock <- api.blockGet(b.hash)
        blockList <- api.blockGetAll
      } yield {
        insertedBlock shouldBe readBlock.get
        blockList shouldBe Set(b.hash)
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
