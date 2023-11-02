package slick.api

import cats.effect.Async
import cats.implicits.toFunctorOps
import sdk.primitive.ByteArray
import slick.syntax.all.*
import slick.{SlickDb, SlickQuery}

import scala.concurrent.ExecutionContext

object SlickApi {
  def apply[F[_]: Async: SlickDb](): F[SlickApi[F]] =
    Async[F].executionContext.map(ec => new SlickApi(ec))
}

class SlickApi[F[_]: Async: SlickDb](ec: ExecutionContext) {
  val queries: SlickQuery = SlickQuery(SlickDb[F].profile, ec)

  def shardGetAll: F[Set[String]] = queries.shardGetAll.run.map(_.toSet)

  def deployerGetAll: F[Set[ByteArray]] = queries.deployerGetAll.run.map(_.map(ByteArray(_)).toSet)

  def deployInsert(d: sdk.data.Deploy): F[Unit] = queries
    .deployInsertIfNot(
      data.Deploy(d.sig.bytes, d.deployerPk.bytes, d.shardName, d.program, d.phloPrice, d.phloLimit, d.nonce),
    )
    .run
    .void

  def deployGetAll: F[Set[ByteArray]] = queries.deployGetAll.run.map(_.map(ByteArray(_)).toSet)

  def deployGet(sig: ByteArray): F[Option[sdk.data.Deploy]] = queries
    .deployGetData(sig.bytes)
    .run
    .map(
      _.map(d =>
        sdk.data.Deploy(
          sig = ByteArray(d.sig),
          deployerPk = ByteArray(d.deployerPk),
          shardName = d.shardName,
          program = d.program,
          phloPrice = d.phloPrice,
          phloLimit = d.phloLimit,
          nonce = d.nonce,
        ),
      ),
    )

  def deployDelete(sig: ByteArray): F[Int] = queries.deployDeleteAndCleanUp(sig.bytes).run

  /** DeploySet */
  def deploySetInsert(deploySetHash: ByteArray, deploySigs: Set[ByteArray]): F[Unit] =
    queries.deploySetInsertIfNot(deploySetHash.bytes, deploySigs.toSeq.map(_.bytes)).run.void

  def deploySetGetAll: F[Set[ByteArray]] = queries.deploySetGetAll.run.map(_.map(ByteArray(_)).toSet)

  def deploySetGet(hash: ByteArray): F[Option[Set[ByteArray]]] = queries
    .deploySetGetData(hash.bytes)
    .run
    .map(_.map(_.map(ByteArray(_)).toSet))

  /** BlockSet */
  def blockSetInsert(blockSetHash: ByteArray, blockHashes: Set[ByteArray]): F[Unit] =
    queries.blockSetInsertIfNot(blockSetHash.bytes, blockHashes.toSeq.map(_.bytes)).run.void

  def blockSetGetAll: F[Set[ByteArray]] = queries.blockSetGetAll.run.map(_.map(ByteArray(_)).toSet)

  def blockSetGet(hash: ByteArray): F[Option[Set[ByteArray]]] = queries
    .blockSetGetData(hash.bytes)
    .run
    .map(_.map(_.map(ByteArray(_)).toSet))

  /** BondsMap */
  def bondsMapInsert(bondsMapHash: ByteArray, bMap: Map[ByteArray, Long]): F[Unit] =
    queries.bondsMapInsertIfNot(bondsMapHash.bytes, bMap.toSeq.map(x => (x._1.bytes, x._2))).run.void

  def bondsMapGetAll: F[Set[ByteArray]] = queries.bondsMapGetAll.run.map(_.map(ByteArray(_)).toSet)

  def bondsMapGet(hash: ByteArray): F[Option[Map[ByteArray, Long]]] = queries
    .bondsMapGetData(hash.bytes)
    .run
    .map(_.map(_.map(x => (ByteArray(x._1), x._2)).toMap))

  def blockInsert(b: sdk.data.Block)(
    justificationSetHash: Option[ByteArray],
    offencesSetHash: Option[ByteArray],
    bondsMapHash: ByteArray,
    finalFringeHash: Option[ByteArray],
    deploySetHash: Option[ByteArray],
    mergeSetHash: Option[ByteArray],
    dropSetHash: Option[ByteArray],
    mergeSetFinalHash: Option[ByteArray],
    dropSetFinalHash: Option[ByteArray],
  ): F[Unit] = queries
    .blockInsertIfNot(
      data.Block(
        version = b.version,
        hash = b.hash.bytes,
        sigAlg = b.sigAlg,
        signature = b.signature.bytes,
        finalStateHash = b.finalStateHash.bytes,
        postStateHash = b.postStateHash.bytes,
        validatorPk = b.validatorPk.bytes,
        shardName = b.shardName,
        justificationSet = justificationSetHash.map(h => data.SetData(h.bytes, b.justificationSet.toSeq.map(_.bytes))),
        seqNum = b.seqNum,
        offencesSet = offencesSetHash.map(h => data.SetData(h.bytes, b.offencesSet.toSeq.map(_.bytes))),
        bondsMap = data.BondsMapData(bondsMapHash.bytes, b.bondsMap.toSeq.map(x => (x._1.bytes, x._2))),
        finalFringe = finalFringeHash.map(h => data.SetData(h.bytes, b.finalFringe.toSeq.map(_.bytes))),
        deploySet = deploySetHash.map(h => data.SetData(h.bytes, b.deploySet.toSeq.map(_.bytes))),
        mergeSet = mergeSetHash.map(h => data.SetData(h.bytes, b.mergeSet.toSeq.map(_.bytes))),
        dropSet = dropSetHash.map(h => data.SetData(h.bytes, b.dropSet.toSeq.map(_.bytes))),
        mergeSetFinal = mergeSetFinalHash.map(h => data.SetData(h.bytes, b.mergeSetFinal.toSeq.map(_.bytes))),
        dropSetFinal = dropSetFinalHash.map(h => data.SetData(h.bytes, b.dropSetFinal.toSeq.map(_.bytes))),
      ),
    )
    .run
    .void

  def blockGetAll: F[Set[ByteArray]] = queries.blockGetAll.run.map(_.map(ByteArray(_)).toSet)

  def blockGet(hash: ByteArray): F[Option[sdk.data.Block]] = queries
    .blockGetData(hash.bytes)
    .run
    .map(
      _.map(b =>
        sdk.data.Block(
          version = b.version,
          hash = ByteArray(b.hash),
          sigAlg = b.sigAlg,
          signature = ByteArray(b.signature),
          finalStateHash = ByteArray(b.finalStateHash),
          postStateHash = ByteArray(b.postStateHash),
          validatorPk = ByteArray(b.validatorPk),
          shardName = b.shardName,
          justificationSet = b.justificationSet.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
          seqNum = b.seqNum,
          offencesSet = b.offencesSet.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
          bondsMap = b.bondsMap.data.map(x => (ByteArray(x._1), x._2)).toMap,
          finalFringe = b.finalFringe.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
          deploySet = b.deploySet.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
          mergeSet = b.mergeSet.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
          dropSet = b.dropSet.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
          mergeSetFinal = b.mergeSetFinal.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
          dropSetFinal = b.dropSetFinal.map(_.data.map(ByteArray(_)).toSet).getOrElse(Set()),
        ),
      ),
    )
}
