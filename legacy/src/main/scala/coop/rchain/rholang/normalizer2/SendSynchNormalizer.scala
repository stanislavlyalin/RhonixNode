package coop.rchain.rholang.normalizer2

import cats.effect.Sync
import io.rhonix.rholang.*
import io.rhonix.rholang.ast.rholang.Absyn.*
import sdk.syntax.all.*

import java.util.UUID
import scala.jdk.CollectionConverters.*

object SendSynchNormalizer {
  def normalizeSendSynch[F[_]: Sync: NormalizerRec](p: PSendSynch): F[ParN] = Sync[F].defer {
    val identifier = UUID.randomUUID().toString
    val nameVar    = new NameVar(identifier)

    val send: PSend = {
      p.listproc_.asScala.prepend(new PEval(nameVar)).void()
      new PSend(p.name_, new SendSingle(), p.listproc_)
    }

    val receive: PInput = {
      val listName = new ListName()
      listName.add(new NameWildcard).void()

      val listLinearBind = new ListLinearBind()
      listLinearBind.add(
        new LinearBindImpl(listName, new NameRemainderEmpty, new SimpleSource(nameVar)),
      ).void()

      val listReceipt = new ListReceipt()
      listReceipt.add(new ReceiptLinear(new LinearSimple(listLinearBind))).void()

      new PInput(
        listReceipt,
        p.synchsendcont_ match {
          case _: EmptyCont               => new PNil()
          case nonEmptyCont: NonEmptyCont => nonEmptyCont.proc_
        },
      )
    }

    val listName = new ListNameDecl()
    listName.add(new NameDeclSimpl(identifier)).void()
    NormalizerRec[F].normalize(new PNew(listName, new PPar(send, receive)))
  }
}
