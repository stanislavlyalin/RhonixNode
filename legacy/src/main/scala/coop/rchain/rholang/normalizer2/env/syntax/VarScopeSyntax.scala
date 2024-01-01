package coop.rchain.rholang.normalizer2.env.syntax

import coop.rchain.rholang.normalizer2.env.{BoundVarWriter, FreeVarWriter}

trait VarScopeSyntax {
  implicit def varScopeSyntaxOps[T](bw: BoundVarWriter[T]): VarScopeSyntaxOps[T] =
    new VarScopeSyntaxOps[T](bw)
}

final class VarScopeSyntaxOps[T](val bw: BoundVarWriter[T]) extends AnyVal {
  def withNewVarScope[R](insideReceive: Boolean = false)(scopeFn: () => R)(implicit fw: FreeVarWriter[T]): R =
    bw.withNewBoundVarScope(() => fw.withNewFreeVarScope(insideReceive)(scopeFn))
}
