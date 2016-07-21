package org.tmoerman.lab

import org.scalatest.{FlatSpec, Matchers}

import scalaz._
import scalaz.syntax._

/**
  * @author Thomas Moerman
  */
class TreeLab extends FlatSpec with Matchers with ToTreeOps {

  sealed trait Orientation
  case object ? extends Orientation
  case object L extends Orientation
  case object R extends Orientation

  case class Interval[T](val o: Orientation,
                         val lo: T,
                         val hi: T,
                         val max:  T) {

  }

  case object Interval {

    def apply[T](lo: T, hi: T): Interval[T] = Interval(?, lo, hi, hi)

  }

  def intersects[T](a: Interval[T], b: Interval[T])(implicit num: Numeric[T]): Boolean = {
    import num._

    b.lo <= a.hi && a.lo <= b.hi
  }

  /**
    * See video by Bob Sedgewick https://www.youtube.com/watch?v=q0QOYtSsTg4
    *
    * Search algorithm:
    *
    *   - if interval in node intersects query, return it
    *   - else if left subtree is null, go right
    *   - else if max endpoint in left subtree is less than lo, go right
    *   - else go left
    *
    * @param query Interval to find intersections with.
    * @param tree The tree of Intervals that is queried.
    * @tparam T Generic type
    * @return Returns an Iterable of Intervals in the tree that intersect the query Interval.
    */
  def search[T](query: Interval[T], tree: Tree[Interval[T]])(implicit num: Numeric[T]): Iterable[Interval[T]] = {
    import num._

    def inner(zipOpt: Option[TreeLoc[Interval[T]]]): List[Interval[T]] = {

      zipOpt match {
        case Some(loc) => {
          val currentInterval = loc.getLabel

          val acc = if (intersects(currentInterval, query)) currentInterval :: Nil else Nil

          val leftOpt  = loc.findChild(_.loc.getLabel.o == L)
          val rightOpt = loc.findChild(_.loc.getLabel.o == R)

          leftOpt match {
            case None => acc ::: inner(rightOpt)

            case Some(left) => {
              if (left.getLabel.max < query.lo)
                acc ::: inner(rightOpt) // skip left tree
              else
                acc ::: inner(leftOpt) ::: inner(rightOpt)
            }
          }
        }

        case None => Nil
      }
    }

    inner(Some(tree.loc))
  }

  /**
    * Overloaded function to query on singular instance of T
    * @param query
    * @param tree
    * @param num
    * @tparam T
    * @return
    */
  def search[T](query: T, tree: Tree[Interval[T]])(implicit num: Numeric[T]): Iterable[Interval[T]] = search(Interval(query, query), tree)

  val myTree =
    Interval(?, 17, 19, 24).node(
      Interval(L, 5, 8, 18).node(
        Interval(L, 4, 8, 8).leaf,
        Interval(R, 15, 18, 18).node(
          Interval(L, 7, 10, 10).leaf)),
      Interval(R, 21, 24, 24).leaf)

  behavior of "Interval"

  it should "calculate intersections correctly" in {
    intersects(Interval(0, 2), Interval(5, 8)) shouldBe false

    intersects(Interval(0, 5), Interval(2, 8)) shouldBe true
    intersects(Interval(0, 8), Interval(2, 5)) shouldBe true
    intersects(Interval(2, 5), Interval(0, 8)) shouldBe true
    intersects(Interval(2, 8), Interval(0, 5)) shouldBe true

    intersects(Interval(5, 8), Interval(0, 2)) shouldBe(false)
  }

  behavior of "search"

  it should "find the intersections with a query interval" in {
    search(Interval(23, 25), myTree) shouldBe Interval(R,21,24,24) :: Nil

    search(Interval(7, 16), myTree) shouldBe Interval(L,5,8,18) :: Interval(L,4,8,8) :: Interval(R,15,18,18) :: Interval(L,7,10,10) :: Nil

    search(Interval(23,23), myTree) shouldBe Interval(R,21,24,24) :: Nil

    search(Interval(18, 21), myTree) shouldBe Interval(?,17,19,24) :: Interval(R,15,18,18) :: Interval(R,21,24,24) :: Nil
  }

  it should "find the intersections with a query value" in {
    search(18, myTree) shouldBe Interval(?,17,19,24) :: Interval(R,15,18,18) :: Nil
  }

}