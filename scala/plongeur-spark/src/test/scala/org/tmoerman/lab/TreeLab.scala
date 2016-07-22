package org.tmoerman.lab

import org.scalatest.{FlatSpec, Matchers}

import scalaz.Show.showFromToString
import scalaz._
import scalaz.syntax._

/**
  * @author Thomas Moerman
  */
class TreeLab extends FlatSpec with Matchers with ToTreeOps {

  sealed trait Orientation
  case object L extends Orientation
  case object R extends Orientation

  case class Interval[T](val lo: T,
                         val hi: T,
                         val o: Option[Orientation] = None,
                         val maxSubHi: Option[T] = None,
                         val minSubLo: Option[T] = None) {

    def isL = o.map(_ == L).getOrElse(false)
    def isR = o.map(_ == R).getOrElse(false)

    def toL: Interval[T] = copy(o = Some(L))
    def toR: Interval[T] = copy(o = Some(R))

    def tuple = (lo, hi)

    override def toString =
      o.map(v => s"$v").getOrElse("") +
      s"$tuple" +
      maxSubHi.map(v => s" max: $v").getOrElse("") +
      minSubLo.map(v => s", min: $v").getOrElse("")

  }

  object Interval {

    def left[T](lo: T, hi: T, max: Option[T] = None, min: Option[T] = None) =
      Interval(lo, hi, maxSubHi = max, minSubLo = min, o = Some(L))

    def right[T](lo: T, hi: T, max: Option[T] = None, min: Option[T] = None) =
      Interval(lo, hi, maxSubHi = max, minSubLo = min, o = Some(R))

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

          val leftOpt  = loc.findChild(_.loc.getLabel.isL)
          val rightOpt = loc.findChild(_.loc.getLabel.isR)

          leftOpt match {
            case None => acc ::: inner(rightOpt)

            case Some(left) => {
              if (left.getLabel.maxSubHi.map(_ < query.lo).getOrElse(false))
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
    * @param list
    * @tparam T
    * @return Returns a triplet of (List, Option, List) where the Option contains the approximately middle element of
    *         the specified list, whereas the Lists are the flanking elements from the specified List.
    */
  def cleave[T](list: List[T]): (List[T], Option[T], List[T]) =
    list match {
      case Nil => (Nil, None, Nil)
      case xs  => xs.splitAt(list.size / 2) match {
        case (left, x :: right) => (left, Some(x), right)
        case _                  => throw new Exception()
      }
    }

  /**
    * @param intervals assumed to be sorted.
    * @tparam T
    * @return Returns a balanced Interval Search Tree constructed from a list of Intervals.
    */
  def create[T](intervals: List[Interval[T]])(implicit num: Numeric[T]): Tree[Interval[T]] = {
    intervals match {
      case l => cleave(l) match {
        case (Nil,  Some(x), Nil) =>
          x.copy(maxSubHi = Some(x.hi), minSubLo = Some(x.lo)).leaf

        case (left, Some(x), Nil) =>
          val leftTree = create(left).loc.modifyLabel(_.toL).toTree

          x.node(leftTree)
            .loc
            .modifyLabel(self =>
              self.copy(
                maxSubHi =
                  Some(
                    List(
                      Some(self.hi),
                      leftTree.loc.getLabel.maxSubHi).flatten.max),
                minSubLo =
                  Some(
                    List(
                      Some(self.lo),
                      leftTree.loc.getLabel.minSubLo).flatten.min)))
            .toTree

        case (left, Some(x), right) =>
          val leftTree  = create(left).loc.modifyLabel(_.toL).toTree
          val rightTree = create(right).loc.modifyLabel(_.toR).toTree

          x.node(leftTree, rightTree)
            .loc.modifyLabel(self =>
              self.copy(
                maxSubHi =
                  Some(
                    List(
                      Some(self.hi),
                      leftTree.loc.getLabel.maxSubHi,
                      rightTree.loc.getLabel.maxSubHi).flatten.max),
                minSubLo =
                  Some(
                    List(
                      Some(self.lo),
                      leftTree.loc.getLabel.minSubLo,
                      rightTree.loc.getLabel.minSubLo).flatten.min)))
            .toTree

        case _ => throw new Exception("dafuq")
      }
    }
  }

  /**
    * Overloaded function to query on singular instance of T
    *
    * @param query
    * @param tree
    * @param num
    * @tparam T
    * @return
    */
  def search[T](query: T, tree: Tree[Interval[T]])(implicit num: Numeric[T]): Iterable[Interval[T]] = search(Interval(query, query), tree)

  import Interval._

  val myTree =
    Interval(17, 19, maxSubHi = Some(24)).node(
      left(5, 8, max = Some(18)).node(
        left(4, 8, max = Some(8)).leaf,
        right(15, 18, max = Some(18)).node(
          left(7, 10, max = Some(10)).leaf)),
      right(21, 24, max = Some(24)).leaf)

  behavior of "Scalaz trees"

  implicit val showInt = showFromToString[Interval[Int]]

  it should "be drawable" in {
    println(myTree.drawTree)
  }

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
    search(Interval(23, 25), myTree).map(_.tuple) shouldBe (21, 24) :: Nil

    search(Interval(7, 16), myTree).map(_.tuple) shouldBe (5, 8) :: (4, 8) :: (15, 18) :: (7, 10) :: Nil

    search(Interval(23,23), myTree).map(_.tuple) shouldBe (21, 24) :: Nil

    search(Interval(18, 21), myTree).map(_.tuple) shouldBe (17, 19) :: (15, 18) :: (21, 24) :: Nil
  }

  it should "find the intersections with a query value" in {
    search(18, myTree).map(_.tuple) shouldBe (17, 19) :: (15, 18) :: Nil
  }

  behavior of "constructing an Interval Tree"

  val intervals = Range(0, 20, 1).map(i => Interval(i*5, i*5 + 10)).toList

  it should "meh" in {
    val tree = create(intervals)

    println(tree.drawTree)

    val result = search(19, tree)

    result.size shouldBe 2

    println(result)
  }

  it should "work naively" in {
    val naive = intervals match {
      case head :: rest =>
        rest.foldLeft(head.leaf.loc){
          (currentTree: TreeLoc[Interval[Int]], interval: Interval[Int]) => currentTree.insertDownLast(interval.leaf)
        }
      case _ => throw new Exception("dafuq")
    }

    println(naive.toTree.drawTree)
  }

  behavior of "cleaving lists"

  it should "work correctly" in {
    cleave(Nil)                 shouldBe (Nil,        None,    Nil)
    cleave(List(1))             shouldBe (Nil,        Some(1), Nil)
    cleave(List(1, 2))          shouldBe (List(1),    Some(2), Nil)
    cleave(List(1, 2, 3, 4))    shouldBe (List(1, 2), Some(3), List(4))
    cleave(List(1, 2, 3, 4, 5)) shouldBe (List(1, 2), Some(3), List(4, 5))
  }

}