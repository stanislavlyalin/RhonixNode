package coop.rchain.rholang.normalizer2.dsl

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import coop.rchain.rholang.normalizer2.envimpl.BundleInfoChain
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BundleInfoChainSpec extends AnyFlatSpec with Matchers {

  "BundleInfoChain" should "initialize with default status" in {
    val chain = BundleInfoChain[IO]()
    chain.getStatus shouldBe false
  }

  it should "set the status to true if the scope function runs" in {
    val chain   = BundleInfoChain[IO]()
    val scopeFn = IO {
      chain.getStatus shouldBe true
    }
    chain.runWithNewStatus(scopeFn).unsafeRunSync()
  }

  it should "maintain the status as true if the scope function runs within the nested scope" in {
    val chain   = BundleInfoChain[IO]()
    val scopeFn = IO {
      chain.getStatus shouldBe true
    }
    chain.runWithNewStatus(chain.runWithNewStatus(scopeFn)).unsafeRunSync()
  }
}
