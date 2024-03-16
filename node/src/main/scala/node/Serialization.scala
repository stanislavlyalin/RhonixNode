package node

import cats.syntax.all.*
import cats.{Applicative, Monad}
import dproc.data.Block
import sdk.api.data.{Balance, TokenTransferRequest}
import sdk.codecs.{PrimitiveReader, PrimitiveWriter, Serialize}
import sdk.data.{BalancesDeploy, BalancesDeployBody, BalancesState}
import sdk.primitive.ByteArray
import weaver.data.{Bonds, ConflictResolution}

import java.net.InetSocketAddress
import scala.math.Numeric.LongIsIntegral

object Serialization {

  // TODO this should be derived? Magnolia?
  implicit def serializeTuple[F[_]: Applicative, A, B](implicit
    sA: Serialize[F, A],
    sB: Serialize[F, B],
  ): Serialize[F, (A, B)] =
    new Serialize[F, (A, B)] {
      override def write(x: (A, B)): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        sA.write(x._1)(w) *> sB.write(x._2)(w)

      override def read: PrimitiveReader[F] => F[(A, B)] = (r: PrimitiveReader[F]) => (sA.read(r), sB.read(r)).tupled
    }

  implicit def serializeOpt[F[_]: Monad, A](implicit serializeA: Serialize[F, A]): Serialize[F, Option[A]] =
    new Serialize[F, Option[A]] {
      override def write(x: Option[A]): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case Some(a) => w.write(true) *> serializeA.write(a)(w)
          case None    => w.write(false)
        }

      override def read: PrimitiveReader[F] => F[Option[A]] = (r: PrimitiveReader[F]) =>
        for {
          exists <- r.readBool
          a      <- if (exists) serializeA.read(r).map(Some(_)) else None.pure[F]
        } yield a
    }

  implicit def seqSerialize[F[_]: Monad, A: Ordering](implicit sA: Serialize[F, A]): Serialize[F, Seq[A]] =
    new Serialize[F, Seq[A]] {
      override def write(x: Seq[A]): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        // just `traverse_` cannot be used
        // See more examples here: https://gist.github.com/nzpr/38f843139224d1de1f0e9d15c75e925c
        w.write(x.size) *> x.sorted.map(sA.write(_)(w)).fold(().pure[F])(_ *> _)

      override def read: PrimitiveReader[F] => F[Seq[A]] = (r: PrimitiveReader[F]) =>
        for {
          size <- r.readInt
          seq  <- (0 until size).toList.traverse(_ => sA.read(r))
        } yield seq
    }

  // TODO generate serializers for all types
  implicit def unitSerialize[F[_]: Applicative]: Serialize[F, Unit] = new Serialize[F, Unit] {
    override def write(x: Unit): PrimitiveWriter[F] => F[Unit] = (_: PrimitiveWriter[F]) => ().pure[F]
    override def read: PrimitiveReader[F] => F[Unit]           = (_: PrimitiveReader[F]) => ().pure[F]
  }

  implicit def longSerialize[F[_]: Applicative]: Serialize[F, Long] = new Serialize[F, Long] {
    override def write(x: Long): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) => w.write(x)
    override def read: PrimitiveReader[F] => F[Long]           = (r: PrimitiveReader[F]) => r.readLong
  }

  implicit def booleanSerialize[F[_]: Monad]: Serialize[F, Boolean] =
    new Serialize[F, Boolean] {
      override def write(x: Boolean): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) => w.write(x)

      override def read: PrimitiveReader[F] => F[Boolean] = (r: PrimitiveReader[F]) =>
        for {
          rcvd <- r.readBool
        } yield rcvd
    }

  implicit def byteArraySerialize[F[_]: Monad]: Serialize[F, ByteArray] =
    new Serialize[F, ByteArray] {
      override def write(x: ByteArray): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) => w.write(x.bytes)

      override def read: PrimitiveReader[F] => F[ByteArray] = (r: PrimitiveReader[F]) =>
        for {
          hash <- r.readBytes
        } yield ByteArray(hash)
    }

  implicit def balanceSerialize[F[_]: Monad]: Serialize[F, Balance] = new Serialize[F, Balance] {
    override def write(x: Balance): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) => w.write(x.x)
    override def read: PrimitiveReader[F] => F[Balance]           = (r: PrimitiveReader[F]) => r.readLong.map(new Balance(_))
  }

  implicit def inetSocketSerialize[F[_]: Monad]: Serialize[F, InetSocketAddress] =
    new Serialize[F, InetSocketAddress] {
      override def write(x: InetSocketAddress): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        w.write(x.getHostName) *> w.write(x.getPort)

      override def read: PrimitiveReader[F] => F[InetSocketAddress] = (r: PrimitiveReader[F]) =>
        for {
          addr <- r.readString
          port <- r.readInt
        } yield new InetSocketAddress(addr, port)
    }

  implicit def balancesDeployBodySerialize[F[_]: Monad]: Serialize[F, BalancesDeployBody] =
    new Serialize[F, BalancesDeployBody] {
      override def write(x: BalancesDeployBody): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesDeployBody(balanceState, vabn) =>
            w.write(vabn) *> balancesStateSerialize[F].write(balanceState)(w)
        }

      override def read: PrimitiveReader[F] => F[BalancesDeployBody] = (r: PrimitiveReader[F]) =>
        for {
          vabn         <- r.readLong
          balanceState <- balancesStateSerialize[F].read(r)
        } yield BalancesDeployBody(balanceState, vabn)
    }

  implicit def balancesDeploySerialize[F[_]: Monad]: Serialize[F, BalancesDeploy] =
    new Serialize[F, BalancesDeploy] {
      override def write(x: BalancesDeploy): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesDeploy(id, body) => w.write(id.bytes) *> balancesDeployBodySerialize[F].write(body)(w)
        }

      override def read: PrimitiveReader[F] => F[BalancesDeploy] = (r: PrimitiveReader[F]) =>
        for {
          id   <- r.readBytes
          body <- balancesDeployBodySerialize[F].read(r)
        } yield BalancesDeploy(ByteArray(id), body)
    }

  implicit def bondsMapSerialize[F[_]: Monad]: Serialize[F, Map[ByteArray, Long]] =
    new Serialize[F, Map[ByteArray, Long]] {
      override def write(bonds: Map[ByteArray, Long]): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        seqSerialize[F, (ByteArray, Long)].write(bonds.toSeq)(w)

      override def read: PrimitiveReader[F] => F[Map[ByteArray, Long]] = (r: PrimitiveReader[F]) =>
        for {
          bonds <- seqSerialize[F, (ByteArray, Long)].read(r)
        } yield bonds.toMap
    }

  implicit def conflictResolutionSerialize[F[_]: Monad]: Serialize[F, ConflictResolution[BalancesDeploy]] =
    new Serialize[F, ConflictResolution[BalancesDeploy]] {
      override def write(x: ConflictResolution[BalancesDeploy]): PrimitiveWriter[F] => F[Unit] =
        (w: PrimitiveWriter[F]) =>
          x match {
            case ConflictResolution(accepted, rejected) =>
              seqSerialize[F, BalancesDeploy].write(accepted.toList.sorted)(w) *>
                seqSerialize[F, BalancesDeploy].write(rejected.toList.sorted)(w)
          }

      override def read: PrimitiveReader[F] => F[ConflictResolution[BalancesDeploy]] =
        (r: PrimitiveReader[F]) =>
          for {
            acceptedSize <- r.readInt
            accepted     <- (0 until acceptedSize).toList.traverse(_ => balancesDeploySerialize.read(r))
            rejectedSize <- r.readInt
            rejected     <- (0 until rejectedSize).toList.traverse(_ => balancesDeploySerialize.read(r))
          } yield ConflictResolution(accepted.toSet, rejected.toSet)
    }

  implicit def blockSerialize[F[_]: Monad]: Serialize[F, Block[ByteArray, ByteArray, BalancesDeploy]] =
    new Serialize[F, Block[ByteArray, ByteArray, BalancesDeploy]] {
      override def write(x: Block[ByteArray, ByteArray, BalancesDeploy]): PrimitiveWriter[F] => F[Unit] =
        (w: PrimitiveWriter[F]) =>
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
                seqSerialize[F, ByteArray].write(minGenJs.toList)(w) *>
                seqSerialize[F, ByteArray].write(offences.toList)(w) *>
                seqSerialize[F, BalancesDeploy].write(txs)(w) *>
                seqSerialize[F, ByteArray].write(finalFringe.toList.sorted)(w) *>
                serializeOpt[F, ConflictResolution[BalancesDeploy]].write(finalized)(w) *>
                seqSerialize[F, BalancesDeploy].write(merge.toList.sorted)(w) *>
                seqSerialize[F, (ByteArray, Long)].write(bonds.bonds.toList.sorted)(w) *>
                w.write(lazTol) *>
                w.write(expThresh) *>
                w.write(finalStateHash.bytes) *>
                w.write(postStateHash.bytes)
          }

      override def read: PrimitiveReader[F] => F[Block[ByteArray, ByteArray, BalancesDeploy]] =
        (x: PrimitiveReader[F]) =>
          for {
            sender        <- x.readBytes
            minGenJs      <- seqSerialize[F, ByteArray].read(x)
            offences      <- seqSerialize[F, ByteArray].read(x)
            txs           <- seqSerialize[F, BalancesDeploy].read(x)
            finalFringe   <- seqSerialize[F, ByteArray].read(x)
            finalized     <- serializeOpt[F, ConflictResolution[BalancesDeploy]].read(x)
            merge         <- seqSerialize[F, BalancesDeploy].read(x)
            bonds         <- seqSerialize[F, (ByteArray, Long)].read(x)
            lazTol        <- x.readInt
            expThresh     <- x.readInt
            finalState    <- x.readBytes.map(ByteArray(_))
            postStateHash <- x.readBytes.map(ByteArray(_))
          } yield Block(
            ByteArray(sender),
            minGenJs.toSet,
            offences.toSet,
            txs.toList,
            finalFringe.toSet,
            finalized,
            merge.toSet,
            Bonds(bonds.toMap),
            lazTol,
            expThresh,
            finalState,
            postStateHash,
          )
    }

  implicit def blockWithIdSerialize[F[_]: Monad]: Serialize[F, Block.WithId[ByteArray, ByteArray, BalancesDeploy]] =
    new Serialize[F, Block.WithId[ByteArray, ByteArray, BalancesDeploy]] {
      override def write(x: Block.WithId[ByteArray, ByteArray, BalancesDeploy]): PrimitiveWriter[F] => F[Unit] =
        (w: PrimitiveWriter[F]) => w.write(x.id.bytes) *> blockSerialize[F].write(x.m)(w)

      override def read: PrimitiveReader[F] => F[Block.WithId[ByteArray, ByteArray, BalancesDeploy]] =
        (r: PrimitiveReader[F]) =>
          for {
            id    <- r.readBytes
            block <- blockSerialize[F].read(r)
          } yield Block.WithId(ByteArray(id), block)
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

  implicit def balancesStateSerialize[F[_]: Monad]: Serialize[F, BalancesState] =
    new Serialize[F, BalancesState] {
      override def write(x: BalancesState): PrimitiveWriter[F] => F[Unit] = (w: PrimitiveWriter[F]) =>
        x match {
          case BalancesState(diffs) => seqSerialize[F, (ByteArray, Long)].write(diffs.toList)(w)
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
}
