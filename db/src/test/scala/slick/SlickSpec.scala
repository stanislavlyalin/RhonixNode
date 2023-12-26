package slick

import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO, Resource}
import cats.syntax.all.*
import org.scalacheck.ScalacheckShapeless.derivedArbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sdk.data.{Block, Deploy}
import sdk.primitive.ByteArray
import slick.SlickSpec.*
import slick.api.SlickApi
import slick.syntax.all.*

class SlickSpec extends AsyncFlatSpec with Matchers with ScalaCheckPropertyChecks {

  "deployInsert() function call" should "add the correct entry to the Deploys, Deployers and Shards table" in {
    forAll { (d: Deploy) =>
      embedPgSlick[IO]
        .use { api =>
          for {
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
        }
        .unsafeRunSync()
    }
  }

  "deployInsert() function call" should "not duplicate records in the Deployers and Shards tables if they are the same" in {
    forAll { (d1: Deploy, d2Sig: ByteArray) =>
      val d2: Deploy = d1.copy(sig = d2Sig) // create d2 with the same fields but another sig
      embedPgSlick[IO]
        .use { api =>
          for {
            _            <- api.deployInsert(d1)
            _            <- api.deployInsert(d2)
            d1FromDB     <- api.deployGet(d1.sig)
            d2FromDB     <- api.deployGet(d2.sig)
            dList        <- api.deployGetAll
            deployerList <- api.deployerGetAll
            shardList    <- api.shardGetAll
          } yield {
            Some(d1) shouldBe d1FromDB
            Some(d2) shouldBe d2FromDB
            dList shouldBe Set(d1.sig, d2.sig)
            deployerList shouldBe Set(d1.deployerPk)
            shardList shouldBe Set(d1.shardName)
          }
        }
        .unsafeRunSync()
    }
  }

  "Concurrent deployInsert() function calls" should "correctly handle concurrency" in {
    // Test inserting the same deploy 100 times in parallel.
    // During insertion all nested fields are first saved if not present (e.g. deploy id, shard id)

    val d       = Arbitrary.arbitrary[Deploy].sample.get
    val sigs    = Gen.listOfN(100, Arbitrary.arbitrary[ByteArray]).sample.get
    val deploys = sigs.map(s => d.copy(sig = s))

    embedPgSlick[IO]
      .use { api =>
        deploys.parTraverse_(api.deployInsert).map(_ shouldBe an[Unit]) >>
          sigs.traverse(api.deployGet).map(_.count(_.isDefined)).map(_ shouldBe sigs.length)
      }
      .unsafeRunSync()
  }

  "deployDelete() function call" should "remove deploy and clean up dependencies in Deployers and Shards tables if possible" in {
    forAll {
      d1: Deploy =>
        val d2Sig      = Arbitrary.arbitrary[ByteArray].suchThat(_ != d1.sig).sample.get
        val d2: Deploy = d1.copy(sig = d2Sig) // create d2 with the same fields but another sig

        embedPgSlick[IO]
          .use { api =>
            for {
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
          }
          .unsafeRunSync()
    }
  }

  "deploySetInsert() function call" should "add the correct entry to the DeploySets and DeploySetBinds tables" in {
    forAll(nonEmptyDeploySeqGen, Arbitrary.arbitrary[ByteArray]) { (deploys: Set[Deploy], dSetHash: ByteArray) =>
      embedPgSlick[IO]
        .use { api =>
          for {
            _         <- deploys.toSeq.traverse(api.deployInsert)
            deploySigs = deploys.map(_.sig)
            _         <- api.deploySetInsert(dSetHash, deploySigs)

            dSet     <- api.deploySetGet(dSetHash)
            dSetList <- api.deploySetGetAll
          } yield {
            deploySigs shouldBe dSet.get
            dSetList shouldBe Set(dSetHash)
          }
        }
        .unsafeRunSync()
    }
  }

  "bondsMapInsert() function call" should "add the correct entry to the BondsMaps and Bonds tables" in {
    forAll(Arbitrary.arbitrary[ByteArray], nonEmptyBondsMapGen) { (bMapHash: ByteArray, bMap: Map[ByteArray, Long]) =>
      embedPgSlick[IO]
        .use { api =>
          for {
            _ <- api.bondsMapInsert(bMapHash, bMap)

            readBMap <- api.bondsMapGet(bMapHash)
            bMapList <- api.bondsMapGetAll
          } yield {
            bMap shouldBe readBMap.get
            bMapList shouldBe Seq(bMapHash).toSet
          }
        }
        .unsafeRunSync()
    }
  }

  "blockInsert() function call" should "add the correct entry to the Blocks table and to the all related tables" in {
    forAll(
      Gen.listOfN(3, Arbitrary.arbitrary[ByteArray]),
      nonEmptyDeploySeqGen,
      nonEmptyBondsMapGen,
      Arbitrary.arbitrary[Block],
      Arbitrary.arbitrary[Block],
      nonEmptyAlphaString,
    ) { (hashes, deploys_, bMap, b1_, b2_, shardName) =>
      val dSetHash = hashes.get(0).get
      val bMapHash = hashes.get(1).get
      val bSetHash = hashes.get(2).get

      val b2Hash  = Arbitrary.arbitrary[ByteArray].suchThat(_ != b1_.hash).sample.get
      val b1      = b1_.copy(shardName = shardName, sigAlg = nonEmptyAlphaString.sample.get)
      val b2      = b2_.copy(hash = b2Hash, shardName = shardName, sigAlg = nonEmptyAlphaString.sample.get)
      val deploys = deploys_.map(_.copy(shardName = shardName, program = nonEmptyAlphaString.sample.get))

      embedPgSlick[IO]
        .use { api =>
          for {
            _             <- deploys.toSeq.traverse(api.deployInsert)
            dSetSigs       = deploys.map(_.sig)
            insertedBlock1 = sdk.data.Block(
                               version = b1.version,
                               hash = b1.hash,
                               sigAlg = b1.sigAlg,
                               signature = b1.signature,
                               finalStateHash = b1.finalStateHash,
                               postStateHash = b1.postStateHash,
                               validatorPk = b1.validatorPk,
                               shardName = b1.shardName,
                               justificationSet = Set(),
                               seqNum = b1.seqNum,
                               offencesSet = Set(),
                               bondsMap = bMap,
                               finalFringe = Set(),
                               execDeploySet = dSetSigs,
                               mergeDeploySet = dSetSigs,
                               dropDeploySet = Set(),
                               mergeDeploySetFinal = Set(),
                               dropDeploySetFinal = Set(),
                             )
            _             <- api.blockInsert(insertedBlock1)(
                               None,
                               None,
                               bMapHash,
                               None,
                               Some(dSetHash),
                               Some(dSetHash),
                               None,
                               None,
                               None,
                             )

            insertedBlock2 = sdk.data.Block(
                               version = b2.version,
                               hash = b2.hash,
                               sigAlg = b2.sigAlg,
                               signature = b2.signature,
                               finalStateHash = b2.finalStateHash,
                               postStateHash = b2.postStateHash,
                               validatorPk = b2.validatorPk,
                               shardName = b2.shardName,
                               justificationSet = Set(b1.hash),
                               seqNum = b2.seqNum,
                               offencesSet = Set(b1.hash),
                               bondsMap = bMap,
                               finalFringe = Set(b1.hash),
                               execDeploySet = Set(),
                               mergeDeploySet = Set(),
                               dropDeploySet = dSetSigs,
                               mergeDeploySetFinal = dSetSigs,
                               dropDeploySetFinal = dSetSigs,
                             )
            _             <- api.blockInsert(insertedBlock2)(
                               Some(bSetHash),
                               Some(bSetHash),
                               bMapHash,
                               Some(bSetHash),
                               None,
                               None,
                               Some(dSetHash),
                               Some(dSetHash),
                               Some(dSetHash),
                             )

            readBlock1 <- api.blockGet(b1.hash)
            readBlock2 <- api.blockGet(b2.hash)
            blockList  <- api.blockGetAll
          } yield {
            readBlock1 shouldBe Some(insertedBlock1)
            readBlock2 shouldBe Some(insertedBlock2)
            blockList shouldBe Set(b1.hash, b2.hash)
          }
        }
        .unsafeRunSync()
    }
  }

  "Stored and loaded name-value pairs" should "be the same" in {
    embedPgSlick[IO]
      .use { api =>
        implicit val slickDb: SlickDb = api.slickDb

        IO.delay {
          forAll(nonEmptyAlphaString, nonEmptyAlphaString) { (name, value) =>
            val storeF = api.queries.putConfig(name, value).run[IO]
            val loadF  = api.queries.getConfig(name).run[IO]
            (storeF >> loadF).unsafeRunSync() shouldBe Some(value)
          }
        }
      }
      .unsafeRunSync()
  }
}

object SlickSpec {

  def embedPgSlick[F[_]: Async]: Resource[F, SlickApi[F]] = EmbeddedPgSqlSlickDb[F].evalMap(SlickApi[F])

  // Define Arbitrary for ByteArray since it's a custom type and needs specific generation logic
  implicit val byteArrayArbitrary: Arbitrary[ByteArray] = Arbitrary {
    Gen.nonEmptyListOf(Gen.alphaChar).map(chars => ByteArray(chars.mkString.getBytes))
  }

  val nonEmptyDeploySeqGen: Gen[Set[Deploy]] = for {
    size    <- Gen.chooseNum(1, 10) // Choose a suitable max value
    deploys <- Gen.listOfN(size, Arbitrary.arbitrary[Deploy])
  } yield deploys.toSet

  val nonEmptyBondsMapGen: Gen[Map[ByteArray, Long]] = for {
    size  <- Gen.chooseNum(1, 10) // Choose a suitable max value
    bonds <- Gen.listOfN(size, Arbitrary.arbitrary[(ByteArray, Long)])
  } yield bonds.toMap

  val nonEmptyAlphaString: Gen[String] = Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString)
}
