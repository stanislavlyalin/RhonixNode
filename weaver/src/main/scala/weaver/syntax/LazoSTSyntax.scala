package weaver.syntax

import cats.data.Kleisli
import weaver.Lazo
import weaver.data.{Bonds, LazoF}
import cats.syntax.all._
import weaver.rules.Dag.computeFJS

trait LazoSTSyntax {
  implicit final def lazoLazoSyntax[M, S](st: Lazo[M, S]) = new LazoSTOps[M, S](st)
}

final class LazoSTOps[M, S](private val s: Lazo[M, S]) {
  // idx of the latest fringe across set of messages
  def lfIdxOpt: Set[M] => Option[Int] = _.map(s.dagData(_: M).fringeIdx).lastOption

  // idx of the latest fringe across set of messages
  def lfUnsafe: Set[M] => LazoF[M] = _.map(s.dagData(_: M).fringeIdx).lastOption.map(s.fringes).map(LazoF(_)).get

  // latest bonds map for a view defined by mgjs
  def bondsMap(mgjs: Set[M]): Option[Bonds[S]] =
    Kleisli(lfIdxOpt)
      .andThen(Kleisli[Option, Int, Bonds[S]](s.exeData(_: Int).bondsMap.some))
      .run(mgjs)

  def fjsOpt(mgjs: Set[M]) = bondsMap(mgjs).map(_.activeSet).map(fullJs(_)(mgjs))

  // derive full justification set from mgjs
  def fullJs(activeSet: Set[S]) =
    (mgjs: Set[M]) =>
      computeFJS(
        mgjs,
        activeSet,
        s.dagData(_: M).jss,
        (x: M, y: M) => s.seenMap.get(x).exists(_.contains(y)),
        s.dagData(_: M).sender
      )

  def selfJOpt(mgjs: Set[M], sender: S): Option[M] =
    bondsMap(mgjs).map(_.activeSet).flatMap(x => selfJOpt(x, sender, mgjs))

  // self justification option
  def selfJOpt(activeSet: Set[S], sender: S, mgjs: Set[M]): Option[M] =
    fullJs(activeSet).andThen(x => x.find(s.dagData(_).sender == sender))(mgjs)

  // all messages seen through view defined by mgjs
  def seen = (mgjs: Set[M]) => mgjs ++ mgjs.flatMap(s.seenMap)
}
