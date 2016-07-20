package org.tmoerman.lab

import org.scalatest.{Matchers, FlatSpec}

import scalaz.Tree._
import scalaz._
import Scalaz._
import scalaz.syntax._

/**
  * @author Thomas Moerman
  */
class TreeLab extends FlatSpec with Matchers with ToTreeOps {

  sealed trait Orientation
  case object L extends Orientation
  case object R extends Orientation

  case class Interval[@specialized(Int, Double) T](val o: Orientation,
                                                   val lo: T,
                                                   val hi: T,
                                                   val v:  T) {

    def intersectsWith(that: Interval[T]): Boolean = ???

  }

  val myTree =
    Interval(L, 17, 19, 24).node(
      Interval(L, 5, 8, 18).node(
        Interval(L, 4, 8, 8).leaf,
        Interval(R, 15, 18, 18).node(
          Interval(L, 7, 10, 10).leaf)),
      Interval(R, 21, 24, 24).leaf)

  /**
    * See video by Bob Sedgewick https://www.youtube.com/watch?v=q0QOYtSsTg4
    *
    * @param query Interval to find intersections with.
    * @param tree The tree of Intervals that is queried.
    * @tparam T Generic type
    * @return Returns an Iterable of Intervals in the tree that intersect the query Interval.
    */
  def search[@specialized(Int, Double) T](query: Interval[T], tree: Tree[Interval[T]]): Iterable[Interval[T]] = {

    val zipperAtRoot: TreeLoc[Interval[T]] = tree.loc.firstChild.get



    ???
  }

  behavior of "interval tree"

  it should "meh" in {

    val zipper = myTree.loc

    zipper.firstChild.get.toTree.rootLabel  shouldBe Interval(L, 17, 19, 24)

    zipper.lastChild.get.toTree.rootLabel   shouldBe Interval(L, 17, 19, 24)

    zipper.getChild(1).get.toTree.rootLabel shouldBe Interval(L, 17, 19, 24)

  }

  it should "be searchable for intersections" in {

    

  }

}