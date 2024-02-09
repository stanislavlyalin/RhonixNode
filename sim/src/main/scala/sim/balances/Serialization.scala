package sim.balances

import cats.Monad
import cats.syntax.all.*
import dproc.data.Block
import sdk.api.data.TokenTransferRequest
import sdk.codecs.{PrimitiveReader, PrimitiveWriter, Serialize}
import sdk.primitive.ByteArray
import sim.balances.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sim.NetworkSim.*
import weaver.data.{Bonds, ConflictResolution}

object Serialization {
  implicit def balancesStateSerialize[F[_]: Monad]: Serialize[F, BalancesState] =
    new Serialize[F, BalancesState] {
      override def write(x: BalancesState): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesState(diffs) =>
            w.write(diffs.size) *>
              w.write[(Wallet, Balance)](diffs.toSeq.sorted, { case (k, v) => w.write(k.bytes) *> w.write(v) })
        }

      override def read: PrimitiveReader[F] => F[BalancesState] = (r: PrimitiveReader[F]) =>
        for {
          size  <- r.readInt
          diffs <- (0 until size).toList.traverse { _ =>
                     for {
                       k <- r.readBytes.map(ByteArray(_))
                       v <- r.readLong
                     } yield k -> v
                   }
        } yield BalancesState(diffs.toMap)
    }

  implicit def balancesDeployBodySerialize[F[_]: Monad]: Serialize[F, BalancesDeployBody] =
    new Serialize[F, BalancesDeployBody] {
      override def write(x: BalancesDeployBody): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesDeployBody(balanceState, vabn) =>
            balancesStateSerialize[F].write(balanceState)(w) *> w.write(vabn)
        }

      override def read: PrimitiveReader[F] => F[BalancesDeployBody] = (r: PrimitiveReader[F]) =>
        for {
          balanceState <- balancesStateSerialize[F].read(r)
          vabn         <- r.readLong
        } yield BalancesDeployBody(balanceState, vabn)
    }

  implicit def balancesDeploySerialize[F[_]: Monad]: Serialize[F, BalancesDeploy] =
    new Serialize[F, BalancesDeploy] {
      override def write(x: T): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesDeploy(id, body) => w.write(id.bytes) *> balancesDeployBodySerialize[F].write(body)(w)
        }

      override def read: PrimitiveReader[F] => F[T] = (r: PrimitiveReader[F]) =>
        for {
          id   <- r.readBytes
          body <- balancesDeployBodySerialize[F].read(r)
        } yield BalancesDeploy(ByteArray(id), body)
    }

  implicit def bondsMapSerialize[F[_]: Monad]: Serialize[F, Bonds[S]] =
    new Serialize[F, Bonds[S]] {
      override def write(x: Bonds[S]): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case Bonds(bonds) =>
            w.write(bonds.size) *>
              w.write[(S, Long)](bonds.toSeq.sorted, { case (k, v) => w.write(k.bytes) *> w.write(v) })
        }

      override def read: PrimitiveReader[F] => F[Bonds[S]] = (r: PrimitiveReader[F]) =>
        for {
          size  <- r.readInt
          bonds <- (0 until size).toList.traverse { _ =>
                     for {
                       k <- r.readBytes.map(ByteArray(_))
                       v <- r.readLong
                     } yield k -> v
                   }
        } yield Bonds(bonds.toMap)
    }

  implicit def blockSerialize[F[_]: Monad]: Serialize[F, Block[M, S, T]] =
    new Serialize[F, Block[M, S, T]] {
      override def write(x: Block[M, S, T]): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case Block(
                sender,
                minGenJs,
                offences,
                txs,
                finalFringe,
                finalized,
                merge,
                bonds,
                lazTol,
                expThresh,
                finalStateHash,
                postStateHash,
              ) =>
            w.write(sender.bytes) *>
              w.write(minGenJs.toSeq.sorted, (x: M) => w.write(x.bytes)) *>
              w.write(offences.toSeq.sorted, (x: M) => w.write(x.bytes)) *>
              w.write(txs.sorted, (x: T) => balancesDeploySerialize.write(x)(w)) *>
              w.write(finalFringe.toSeq.sorted, (x: M) => w.write(x.bytes)) *>
              finalized.traverse_ { case ConflictResolution(accepted, rejected) =>
                w.write(accepted.toSeq.sorted, (x: T) => balancesDeploySerialize.write(x)(w)) *>
                  w.write(rejected.toSeq.sorted, (x: T) => balancesDeploySerialize.write(x)(w))
              } *>
              w.write(merge.toSeq.sorted, (x: T) => balancesDeploySerialize.write(x)(w)) *>
              bondsMapSerialize.write(bonds)(w) *>
              w.write(lazTol) *>
              w.write(expThresh) *>
              w.write(finalStateHash) *>
              w.write(postStateHash)
        }

      override def read: PrimitiveReader[F] => F[Block[M, S, T]] = ??? // not required for now

    }

  implicit def tokenTransferRequestBodySerialize[F[_]: Monad]: Serialize[F, TokenTransferRequest.Body] =
    new Serialize[F, TokenTransferRequest.Body] {
      override def write(x: TokenTransferRequest.Body): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case TokenTransferRequest.Body(sender, recipient, tokenId, amount, vafn) =>
            w.write(sender) *>
              w.write(recipient) *>
              w.write(tokenId) *>
              w.write(amount) *>
              w.write(vafn)
        }

      override def read: PrimitiveReader[F] => F[TokenTransferRequest.Body] = (r: PrimitiveReader[F]) =>
        for {
          sender    <- r.readBytes
          recipient <- r.readBytes
          tokenId   <- r.readLong
          amount    <- r.readLong
          vafn      <- r.readLong
        } yield TokenTransferRequest.Body(sender, recipient, tokenId, amount, vafn)
    }

  implicit def tokenTransferRequestSerialize[F[_]: Monad]: Serialize[F, TokenTransferRequest] =
    new Serialize[F, TokenTransferRequest] {
      override def write(x: TokenTransferRequest): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case TokenTransferRequest(pubKey, digest, signature, signatureAlg, body) =>
            w.write(pubKey) *>
              w.write(digest) *>
              w.write(signature) *>
              w.write(signatureAlg) *>
              tokenTransferRequestBodySerialize[F].write(body)(w)
        }

      override def read: PrimitiveReader[F] => F[TokenTransferRequest] = (r: PrimitiveReader[F]) =>
        for {
          pubKey       <- r.readBytes
          digest       <- r.readBytes
          signature    <- r.readBytes
          signatureAlg <- r.readString
          body         <- tokenTransferRequestBodySerialize[F].read(r)
        } yield TokenTransferRequest(pubKey, digest, signature, signatureAlg, body)
    }
}
