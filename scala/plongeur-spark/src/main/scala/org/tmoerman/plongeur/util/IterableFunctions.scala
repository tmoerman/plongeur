package org.tmoerman.plongeur.util

/**
  * @author Thomas Moerman
  */
object IterableFunctions {

  /**
    * @param ordering Implicit Ordering on type T
    * @tparam T The generic type with an implicit Ordering.
    * @tparam IterableLike Generic type for collections that inherit from Iterable.
    *
    * @return Returns an Ordering defined on IterableLike collections of a generic type T with implicit Ordering.
    */
  implicit def pimpIterableOrdering[T, IterableLike[T] <: Iterable[T]](implicit ordering: Ordering[T]): Ordering[IterableLike[T]] =
    new Ordering[IterableLike[T]] {

      def compare(v1: IterableLike[T], v2: IterableLike[T]): Int = {
        (v1.toStream zip v2.toStream)
          .dropWhile{ case (e1, e2) => ordering.compare(e1, e2) == 0 } match {
          case (a, b) #:: _ => ordering.compare(a, b)
          case _            => 0
        }
      }
    }

  implicit def iteratorToIterable[V](it: Iterator[V]): Iterable[V] = it.toIterable

  /**
    * @param it An Iterable
    * @param selector Selector function, defaults to the identity function.
    *
    * @return List of Lists of V instances,
    *         grouping repeats of the result of the selector function applied to the V instances.
    */
  def groupRepeats[V](it: Iterable[V], selector: (V) => Any = (v: V) => v): List[List[V]] =
    it.foldLeft(List[List[V]]()) { (acc, i) =>
      acc match {
        case l :: ls => l match {
          case x :: _ if selector(x) == selector(i) => (i :: l) :: ls
          case _                                    => (i :: Nil) :: acc
        }
        case _ => (i :: Nil) :: Nil
      }}.reverse.map(_.reverse)

}
