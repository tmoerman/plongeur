package org.tmoerman.plongeur.util

/**
  * @author Thomas Moerman
  */
object IterableFunctions {

  /**
    * @param ordering Implicit Ordering on type T.
    * @tparam T The generic type with an implicit Ordering.
    * @tparam IterableLike Generic type for collections that inherit from Iterable.
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

  implicit def iteratorToIterable[T](it: Iterator[T]): Iterable[T] = it.toIterable

  implicit def iterableToIterator[T](it: Iterable[T]): Iterator[T] = it.toIterator

  implicit def pimpIterable[T](it: Iterable[T]): IterableFunctions[T] = new IterableFunctions[T](it)

}

class IterableFunctions[T](it: Iterable[T]) extends Serializable {

  /**
    * @return Returns a Map of frequencies of the elements.
    */
  def frequencies: Map[T, Int] =
    it.foldLeft(Map[T, Int]() withDefaultValue 0) { (acc, v) => acc.updated(v, acc(v) + 1) }

  /**
    * @return Returns an Iterable of sliding pairs assuming step size 1.
    */
  def slidingPairs: Iterable[(T, T)] =
    it.sliding(2).map(t => (t.head, t.tail.head)).toIterable

  /**
    * @return
    */
  def cartesian: Iterable[(T, T)]  = for (
    a <- it;
    b <- it; if a != b) yield (a, b)

  /**
    * @param selector Selector function, defaults to the identity function.
    * @return List of Lists of T instances,
    *         grouping repeats of the result of the selector function applied to the T instances.
    */
  def groupRepeats(selector: (T) => Any = (v: T) => v): List[List[T]] = groupWhile((a: T, b: T) => selector(a) == selector(b))

  /**
    * @param invariant The functional invariant for consecutive elements in the specified list
    * @return Returns a List of Lists, where the nested lists group consecutive elements where the functional invariant
    *         is maintained.
    *
    *         Example:
    *
    *         val list = List(0, 1, 2, 0, 1, 4, 2, 7, 9, 1)
    *
    *         groupWhile[Int](_ < _)(list)  yields  List(List(0, 1, 2), List(0, 1, 4), List(2, 7, 9), List(1))
    */
  def groupWhile(invariant: (T, T) => Boolean): List[List[T]]  =
    it
      .foldLeft(List[List[T]]()) {
        case (Nil, i) => (i :: Nil) :: Nil

        case ((head@(x :: _)) :: tail, t) =>
          if (invariant(x, t))
            (t :: head) :: tail
          else
            (t :: Nil) :: head :: tail

        case _ => Nil
      }
      .reverseMap(_.reverse)

}
