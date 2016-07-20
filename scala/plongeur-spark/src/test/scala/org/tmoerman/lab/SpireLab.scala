package org.tmoerman.lab

import org.scalatest.{Matchers, FlatSpec}
import spire.math.Searching

/**
  * @author Thomas Moerman
  */
class SpireLab extends FlatSpec with Matchers {

  case class Interval[T](val lo: T, val hi: T)

  behavior of "binary searching"

  it should "retrieve entries" in {

    import spire.syntax.order._

    val intervals = Range(0, 20, 1).map(i => Interval(i*5, i*5 + 10)).toArray

    val result = Searching.search(intervals.map(_.lo), 32)

    println(result)

  }



}