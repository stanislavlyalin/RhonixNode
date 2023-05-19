package dproc

import cats.effect.{Ref, Sync}
import dproc.data.Block
import weaver.{Lazo, Weaver}
import cats.syntax.all._

final case class Receiver()

object Receiver {
  // decide what to do with block received
  def add[F[_]: Sync, M, S, T](
    block: Block.Identifier[M, S],
    stRef: Ref[F, Weaver[M, S, T]],
    buRef: Ref[F, Buffer.ST[M]]
  ): F[Unit] = for {
    // order matters here, since on transitioning from buffer to state message first is added to state
    // and only then removed from buffer
    buffer <- buRef.get
    weaver <- stRef.get
  } yield {
    val dup = weaver.lazo.dagData.contains(block.id) || buffer.contains(block.id)
    lazy val add = Lazo.canAdd(block.minGenJs, block.sender, weaver.lazo)

    if (dup) /*ignore*/ ??? else if (add) ??? /*add*/ else ??? /*toBuffer*/
  }
}
