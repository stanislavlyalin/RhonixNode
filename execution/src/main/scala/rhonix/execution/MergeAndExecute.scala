package rhonix.execution
@FunctionalInterface
trait MergePreState[F[_], T] {
  def mergePreState(finalSet: Set[T], toMerge: Set[T], toExecute: Set[T]): F[Boolean]
}
